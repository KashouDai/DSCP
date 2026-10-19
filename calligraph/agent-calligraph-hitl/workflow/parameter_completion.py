def plan_parameter_completion(
    required_parameters: list[str],
    parameters: dict,
    auxiliary_tools: list[str],
    depth: int = 0,
) -> dict:
    """Plan at most one auxiliary lookup without executing it."""
    missing = [name for name in required_parameters if name not in parameters]
    if not missing:
        return {"status": "complete", "missing_parameters": [], "auxiliary_tool": None}
    if depth >= 1:
        return {"status": "blocked", "missing_parameters": missing, "auxiliary_tool": None}
    candidates = list(dict.fromkeys(auxiliary_tools))
    if len(candidates) == 1:
        return {"status": "need_confirmation", "missing_parameters": missing, "auxiliary_tool": candidates[0]}
    return {
        "status": "blocked" if not candidates else "need_user_choice",
        "missing_parameters": missing,
        "auxiliary_tool": None,
    }
