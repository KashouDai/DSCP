def merge_auxiliary_result(
    parameters: dict,
    auxiliary_result: dict,
    required_parameters: list[str],
    allowed_parameters: list[str] | None = None,
) -> dict:
    """Merge only allowed auxiliary fields and report remaining required fields."""
    allowed = set(allowed_parameters or required_parameters)
    merged = dict(parameters)
    for name, value in auxiliary_result.items():
        if name in allowed and name not in merged:
            merged[name] = value
    missing = [name for name in required_parameters if name not in merged]
    return {"parameters": merged, "missing_parameters": missing, "complete": not missing}
