def next_loop_status(state: dict, max_steps: int = 8) -> str:
    """Return the next orchestration phase without performing side effects."""
    executed_tools = state.get("executed_tools", [])
    steps = len(executed_tools)
    if steps >= max_steps:
        return "finished"
    if len(set(executed_tools)) != steps:
        return "finished"
    if state.get("is_single") or not state.get("next_subtask"):
        return "finished"
    return "continue"
