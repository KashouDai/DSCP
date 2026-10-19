def apply_auxiliary_mapping(
    parameters: dict,
    auxiliary_result: dict,
    field_mapping: dict[str, str],
    required_parameters: list[str],
) -> dict:
    """Map declared auxiliary response fields into missing main-tool parameters."""
    merged = dict(parameters)
    errors = []
    for source, target in field_mapping.items():
        if target in merged:
            continue
        if target not in required_parameters:
            errors.append(f"目标参数不在主工具必填参数中: {target}")
            continue
        if source not in auxiliary_result:
            errors.append(f"辅助结果缺少字段: {source}")
            continue
        merged[target] = auxiliary_result[source]
    missing = [name for name in required_parameters if name not in merged]
    return {"parameters": merged, "missing_parameters": missing, "errors": errors}
