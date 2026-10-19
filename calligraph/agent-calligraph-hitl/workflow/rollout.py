from dataclasses import dataclass


@dataclass(frozen=True)
class RolloutReport:
    enabled: bool
    initialized: bool
    passed: bool
    fallback: bool
    reason: str


def run_readonly_rollout(smoke_runner, query: str, readonly_tools: set[str] | None = None) -> RolloutReport:
    readonly_tools = readonly_tools or set()
    report = smoke_runner(query)
    tool_name = report.get("tool_name")
    if tool_name and tool_name not in readonly_tools:
        return RolloutReport(True, True, False, True, "灰度请求未通过只读工具白名单")
    passed = bool(report.get("passed"))
    return RolloutReport(True, True, passed, not passed, report.get("reason", ""))


def can_retire_legacy(reports: list[RolloutReport], minimum_successes: int = 10) -> bool:
    return len(reports) >= minimum_successes and all(report.passed and not report.fallback for report in reports)
