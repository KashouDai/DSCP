from workflow.loop import next_loop_status


def decide_completion(next_task_chain, state: dict, max_steps: int = 8) -> dict:
    if next_loop_status(state, max_steps) == "finished":
        return {"status": "finished", "next_subtask": None}

    decision = next_task_chain.decide_next_task(
        state.get("prompt", ""),
    )
    if decision.is_complete:
        return {"status": "finished", "next_subtask": None}
    return {"status": "continue", "next_subtask": decision.next_subtask}
