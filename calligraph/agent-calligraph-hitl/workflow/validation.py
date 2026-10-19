from dataclasses import dataclass

from workflow.feature_flags import is_langgraph_enabled


@dataclass(frozen=True)
class StartupValidation:
    enabled: bool
    ready: bool
    route: str
    reason: str


def validate_startup(runner=None, enabled_value: str | None = None) -> StartupValidation:
    enabled = is_langgraph_enabled(enabled_value)
    if not enabled:
        return StartupValidation(False, True, "legacy", "USE_LANGGRAPH 未开启")
    if runner is None:
        return StartupValidation(True, False, "legacy", "Graph runner 未初始化")
    return StartupValidation(True, True, "langgraph", "Graph runner 已就绪")
