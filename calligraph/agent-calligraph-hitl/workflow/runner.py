from workflow.adapter import GraphTaskAdapter
from workflow.recovery import recover_state
import json

from workflow.auxiliary_mapping import apply_auxiliary_mapping
from workflow.loop_transition import transition_after_execution


class LangGraphRunner:
    def __init__(self, planning_graph, confirmation_graph, execution_node, adapter=None, next_task_chain=None):
        self.planning_graph = planning_graph
        self.confirmation_graph = confirmation_graph
        self.execution_node = execution_node
        self.adapter = adapter or GraphTaskAdapter()
        self.next_task_chain = next_task_chain

    def start(self, query: str, metadata: dict | None = None, thread_id: str | None = None):
        state = self.adapter.start(query, metadata)
        config = {"configurable": {"thread_id": thread_id}} if thread_id else None
        return self.planning_graph.invoke(state, config=config) if config else self.planning_graph.invoke(state)

    def feedback(self, state: dict, text: str):
        updated = {
            **state,
            **self.confirmation_graph.invoke(self.adapter.feedback(state, text)),
        }
        if updated.get("status") == "confirmed":
            if updated.get("auxiliary_tool"):
                return self._execute_auxiliary(updated)
            executed = {**updated, **self.execution_node(updated)}
            if self.next_task_chain is not None:
                return transition_after_execution(self.next_task_chain, executed)
            return executed
        return updated

    def _execute_auxiliary(self, state: dict):
        if not state.get("auxiliary_parameters"):
            return {**state, "status": "blocked", "error": "辅助工具参数尚未准备完成，已阻止执行"}
        auxiliary_state = {
            **state,
            "selected_tool": state["auxiliary_tool"],
            "parameters": state["auxiliary_parameters"],
            "status": "confirmed",
        }
        result = self.execution_node(auxiliary_state)
        if result.get("status") != "executed":
            return {**state, **result, "status": "blocked", "error": result.get("error", "辅助工具执行失败")}
        try:
            auxiliary_result = json.loads(result["execution_result"]["text"])
        except (KeyError, TypeError, json.JSONDecodeError) as exc:
            return {**state, "status": "blocked", "error": f"辅助工具返回结果无法解析: {exc}"}
        mapping = state.get("auxiliary_mapping", {})
        mapped = apply_auxiliary_mapping(
            state.get("parameters", {}),
            auxiliary_result,
            mapping,
            state.get("required_parameters", []),
        )
        if mapped["errors"] or mapped["missing_parameters"]:
            return {**state, "status": "blocked", "error": "; ".join(mapped["errors"]) or "辅助结果仍缺少主工具参数"}
        return {
            **state,
            "parameters": mapped["parameters"],
            "auxiliary_result": auxiliary_result,
            "auxiliary_tool": None,
            "auxiliary_parameters": None,
            "status": "wait_confirm",
            "execution_result": result.get("execution_result"),
            "executed_tools": result.get("executed_tools", state.get("executed_tools", [])),
            "nodes": result.get("nodes", state.get("nodes", [])),
            "edges": result.get("edges", state.get("edges", [])),
        }

    def resume(self, thread_id: str):
        return recover_state(self.planning_graph, thread_id)
