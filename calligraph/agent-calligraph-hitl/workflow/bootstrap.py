from workflow.factory import create_langgraph_runner


def bootstrap_langgraph_runner(model, tool_manager, tool_use_hub, checkpointer=None):
    """Build a runner without opening any external connection by itself."""

    def resolve_tool(tool_name):
        tools = tool_manager.get_tools_by_operationIds([tool_name])
        if not tools:
            raise ValueError(f"未找到工具: {tool_name}")
        return tools[0]

    return create_langgraph_runner(model, resolve_tool, tool_use_hub, checkpointer=checkpointer)
