from workflow.state import AgentState


class GraphTaskAdapter:
    """Translate between the Graph state and the legacy task API shape."""

    def start(self, query: str, metadata: dict | None = None) -> AgentState:
        state: AgentState = {
            "prompt": query,
            "user_feedback": None,
            "status": "init",
            "error": None,
        }
        if metadata:
            state.update({key: value for key, value in metadata.items() if key in {
                "candidate_tools", "allowed_parameters", "required_parameters"
            }})
        return state

    def feedback(self, state: AgentState, text: str) -> AgentState:
        return {**state, "user_feedback": text}

    def to_legacy_status(self, state: AgentState) -> dict:
        status = state.get("status")
        return {
            "status": {
                "wait_confirm": 100,
                "executed": -1,
                "aborted": -1,
                "blocked": -1,
                "failed": -1,
            }.get(status, 1),
            "systemOutput": state.get("error") or status,
            "isSuccess": status == "executed",
        }
