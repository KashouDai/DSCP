from chains import (
    HumanIntentChain,
    AuxiliaryParamExtractionChain,
    ParamExtractionChain,
    SecurityChain,
    TaskChain,
    ToolSelectionChain,
)
from workflow.execution import build_execution_node
from workflow.graph import build_confirmation_graph, build_planning_graph
from workflow.runner import LangGraphRunner


def create_langgraph_runner(model, tool_resolver, tool_use_hub, checkpointer=None):
    task_chain = TaskChain(model)
    tool_chain = ToolSelectionChain(model)
    param_chain = ParamExtractionChain(model)
    auxiliary_param_chain = AuxiliaryParamExtractionChain(model)
    security_chain = SecurityChain(model)
    intent_chain = HumanIntentChain(model)

    planning_graph = build_planning_graph(
        task_chain,
        tool_chain,
        param_chain,
        security_chain,
        auxiliary_param_chain=auxiliary_param_chain,
        checkpointer=checkpointer,
    )
    confirmation_graph = build_confirmation_graph(intent_chain)
    execution_node = build_execution_node(tool_use_hub, tool_resolver)
    return LangGraphRunner(planning_graph, confirmation_graph, execution_node, next_task_chain=task_chain)
