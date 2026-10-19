from typing import Any, TypedDict


class AgentState(TypedDict, total=False):
    prompt: str
    user_feedback: str | None
    candidate_tools: list[str]
    allowed_parameters: list[str]
    required_parameters: list[str]
    auxiliary_tools: list[str]
    auxiliary_tool: str | None
    missing_parameters: list[str]
    auxiliary_result: dict[str, Any] | None
    auxiliary_mapping: dict[str, str]
    auxiliary_parameters: dict[str, Any] | None
    auxiliary_allowed_parameters: list[str]
    auxiliary_required_parameters: list[str]
    is_single: bool
    current_subtask: str | None
    selected_tool: str | None
    parameters: dict[str, Any]
    should_block: bool
    security_reason: str | None
    status: str
    error: str | None
    execution_result: dict[str, object] | None
    execution_key: str | None
    executed_tools: list[str]
    nodes: list[dict[str, object]]
    edges: list[dict[str, object]]
    summary: str | None

