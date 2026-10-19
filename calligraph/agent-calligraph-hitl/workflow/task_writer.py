class TaskRecordWriter:
    def __init__(self, task_manager):
        self.task_manager = task_manager

    def write(self, task_id: str, state: dict) -> str:
        status = state.get("status")
        legacy_status = {"wait_confirm": 100}.get(status, -1 if status in {
            "executed", "finished", "aborted", "blocked", "failed"
        } else 1)
        output = state.get("summary") or state.get("error") or status or ""
        graph_title = "异常调用链" if status in {"failed", "blocked"} else "正常调用链"
        return self.task_manager.update_task_recorder(
            task_id,
            legacy_status,
            output,
            graph_title=graph_title,
            nodes=state.get("nodes", []),
            edges=state.get("edges", []),
            curr_task_desc=state.get("current_subtask", ""),
            curr_tool_param=state.get("parameters", {}),
        )
