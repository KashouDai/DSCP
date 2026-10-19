from langgraph.graph import END, START, StateGraph

from workflow.state import AgentState
from workflow.parameter_completion import plan_parameter_completion
from workflow.parameter_merge import merge_auxiliary_result
from workflow.auxiliary_mapping import apply_auxiliary_mapping


def build_planning_graph(task_chain, tool_chain, param_chain, security_chain, auxiliary_param_chain=None, checkpointer=None):
    """Build the side-effect-free planning graph."""

    def classify(state: AgentState) -> AgentState:
        decision = task_chain.classify(state["prompt"])
        return {
            "is_single": decision.is_single,
            "current_subtask": decision.first_subtask or state["prompt"],
            "status": "planning",
        }

    def select_tool(state: AgentState) -> AgentState:
        selection = tool_chain.select(
            state["current_subtask"] or state["prompt"],
            state.get("candidate_tools", []),
        )
        return {"selected_tool": selection.tool_name}

    def extract_parameters(state: AgentState) -> AgentState:
        extraction = param_chain.extract(
            state["current_subtask"] or state["prompt"],
            state.get("allowed_parameters", []),
            state.get("required_parameters", []),
            validate_required=not state.get("auxiliary_tools"),
        )
        return {"parameters": extraction.parameters}

    def plan_completion(state: AgentState) -> AgentState:
        parameters = state.get("parameters", {})
        if state.get("auxiliary_result"):
            if state.get("auxiliary_mapping"):
                mapped = apply_auxiliary_mapping(
                    parameters,
                    state["auxiliary_result"],
                    state["auxiliary_mapping"],
                    state.get("required_parameters", []),
                )
                parameters = mapped["parameters"]
            else:
                merged = merge_auxiliary_result(
                    parameters,
                    state["auxiliary_result"],
                    state.get("required_parameters", []),
                    state.get("allowed_parameters", []),
                )
                parameters = merged["parameters"]
        decision = plan_parameter_completion(
            state.get("required_parameters", []),
            parameters,
            state.get("auxiliary_tools", []),
        )
        if decision["status"] == "complete":
            return {"status": "wait_confirm", "parameters": parameters, "missing_parameters": []}
        if decision["status"] == "need_confirmation":
            return {
                "status": "wait_confirm",
                "parameters": parameters,
                "auxiliary_tool": decision["auxiliary_tool"],
                "missing_parameters": decision["missing_parameters"],
            }
        return {
            "status": "blocked",
            "error": "缺少必填参数且无法唯一确定辅助工具",
            "missing_parameters": decision["missing_parameters"],
        }

    def extract_auxiliary_parameters(state: AgentState) -> AgentState:
        if auxiliary_param_chain is None or not state.get("auxiliary_tools"):
            return {}
        try:
            extraction = auxiliary_param_chain.extract(
                state.get("current_subtask") or state["prompt"],
                state.get("auxiliary_allowed_parameters", []),
                state.get("auxiliary_required_parameters", []),
            )
        except ValueError as exc:
            return {"status": "blocked", "error": str(exc)}
        return {"auxiliary_parameters": extraction.parameters}

    def security_check(state: AgentState) -> AgentState:
        decision = security_chain.check(str(state.get("parameters", {})))
        return {
            "should_block": decision.should_block,
            "security_reason": decision.reason,
            "status": "blocked" if decision.should_block else "wait_confirm",
        }

    graph = StateGraph(AgentState)
    graph.add_node("classify", classify)
    graph.add_node("select_tool", select_tool)
    graph.add_node("extract_parameters", extract_parameters)
    graph.add_node("security_check", security_check)
    graph.add_node("extract_auxiliary_parameters", extract_auxiliary_parameters)
    graph.add_node("plan_completion", plan_completion)
    graph.add_edge(START, "classify")
    graph.add_edge("classify", "select_tool")
    graph.add_edge("select_tool", "extract_parameters")
    graph.add_edge("extract_parameters", "security_check")
    graph.add_edge("security_check", "extract_auxiliary_parameters")
    graph.add_edge("extract_auxiliary_parameters", "plan_completion")
    graph.add_edge("plan_completion", END)
    return graph.compile(checkpointer=checkpointer)


def build_confirmation_graph(human_intent_chain, checkpointer=None):
    """Build the isolated human-confirmation graph."""

    def classify_intent(state: AgentState) -> AgentState:
        intent = human_intent_chain.classify(state["prompt"])
        status = {
            "confirm": "confirmed",
            "abort": "aborted",
            "unclear": "wait_confirm",
        }[intent.intent]
        return {"status": status}

    graph = StateGraph(AgentState)
    graph.add_node("classify_intent", classify_intent)
    graph.add_edge(START, "classify_intent")
    graph.add_edge("classify_intent", END)
    return graph.compile(checkpointer=checkpointer)


def build_execution_graph(execution_node, checkpointer=None):
    graph = StateGraph(AgentState)
    graph.add_node("execute_tool", execution_node)
    graph.add_edge(START, "execute_tool")
    graph.add_edge("execute_tool", END)
    return graph.compile(checkpointer=checkpointer)


def build_summary_graph(summary_chain, checkpointer=None):
    def summarize(state: AgentState) -> AgentState:
        result = summary_chain.summarize(str(state.get("execution_result", {})))
        return {"summary": result.answer, "status": "finished"}

    graph = StateGraph(AgentState)
    graph.add_node("summarize", summarize)
    graph.add_edge(START, "summarize")
    graph.add_edge("summarize", END)
    return graph.compile(checkpointer=checkpointer)


def build_calligraph_graph(
    planning_graph,
    confirmation_graph,
    execution_node,
    checkpointer=None,
):
    """Compose planning, human confirmation, and conditional execution."""

    def planning(state: AgentState) -> AgentState:
        return planning_graph.invoke(state)

    def confirmation(state: AgentState) -> AgentState:
        feedback = state.get("user_feedback")
        if feedback is None:
            return state
        return confirmation_graph.invoke({**state, "prompt": feedback})

    def execute(state: AgentState) -> AgentState:
        return execution_node(state)

    def route_after_confirmation(state: AgentState) -> str:
        return "execute" if state.get("status") == "confirmed" else "end"

    graph = StateGraph(AgentState)
    graph.add_node("planning", planning)
    graph.add_node("confirmation", confirmation)
    graph.add_node("execute", execute)
    graph.add_edge(START, "planning")
    graph.add_edge("planning", "confirmation")
    graph.add_conditional_edges(
        "confirmation",
        route_after_confirmation,
        {"execute": "execute", "end": END},
    )
    graph.add_edge("execute", END)
    return graph.compile(checkpointer=checkpointer)
