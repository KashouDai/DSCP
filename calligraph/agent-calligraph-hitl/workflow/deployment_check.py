def validate_rollout_config(config: dict) -> list[str]:
    errors = []
    if config.get("USE_LANGGRAPH") not in {"0", "1", "true", "false"}:
        errors.append("USE_LANGGRAPH 配置无效")
    if not config.get("readonly_tools"):
        errors.append("只读工具白名单为空")
    threshold = config.get("rollback_threshold", 0.2)
    if not 0 <= threshold <= 1:
        errors.append("rollback_threshold 必须在 0 到 1 之间")
    return errors
