from workflow.completion import decide_completion
from workflow.loop import next_loop_status


def transition_after_execution(next_task_chain, state: dict, max_steps: int = 8) -> dict:
    """Decide whether execution finishes or produces the next bounded subtask."""
    if state.get("status") != "executed":
        return state
    if next_loop_status(state, max_steps) == "finished":
        return {**state, "status": "finished", "next_subtask": None}
    decision = decide_completion(next_task_chain, state, max_steps)
    return {**state, **decision}
