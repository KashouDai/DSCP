import json

from workflow.state import AgentState


def build_execution_node(tool_use_hub, tool_resolver):
    """Create a confirmed-only tool execution node."""
    executed_keys: set[str] = set()

    def execute(state: AgentState) -> AgentState:
        if state.get("status") != "confirmed":
            raise ValueError("只有 confirmed 状态允许执行工具")

        tool_name = state.get("selected_tool")
        if not tool_name:
            raise ValueError("执行工具不能为空")

        key = json.dumps(
            [tool_name, state.get("parameters", {})],
            ensure_ascii=False,
            sort_keys=True,
        )
        if key in executed_keys:
            return {"execution_key": key, "status": "executed"}

        try:
            tool = tool_resolver(tool_name)
            response = tool_use_hub.tool_use(tool, dict(state.get("parameters", {})))
        except Exception as exc:
            return {
                "execution_key": key,
                "status": "failed",
                "error": f"工具执行失败: {exc}",
            }
        executed_keys.add(key)
        node = {
            "id": str(len(state.get("nodes", []))),
            "name": tool_name,
            "params": dict(state.get("parameters", {})),
        }
        nodes = [*state.get("nodes", []), node]
        edges = [*state.get("edges", [])]
        if nodes[:-1]:
            edges.append({"source": nodes[-2]["id"], "target": node["id"], "type": "正向API规划"})
        return {
            "execution_key": key,
            "execution_result": {
                "status_code": response.status_code,
                "text": response.text,
            },
            "status": "executed",
            "executed_tools": [*state.get("executed_tools", []), tool_name],
            "nodes": nodes,
            "edges": edges,
        }

    return execute
