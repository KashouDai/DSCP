def run_readonly_smoke(runner, query: str, feedback: str = "确认") -> dict:
    state = runner.start(query, {})
    initial_status = state.get("status")
    if initial_status != "wait_confirm":
        return {
            "passed": False,
            "initial_status": initial_status,
            "final_status": initial_status,
            "reason": "规划阶段未进入等待确认",
        }

    final_state = runner.feedback(state, feedback)
    final_status = final_state.get("status")
    passed = final_status in {"executed", "finished"}
    return {
        "passed": passed,
        "initial_status": initial_status,
        "final_status": final_status,
        "reason": "只读灰度冒烟通过" if passed else "确认后未完成执行",
    }
