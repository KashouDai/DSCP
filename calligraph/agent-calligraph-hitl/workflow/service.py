from concurrent.futures import ThreadPoolExecutor, TimeoutError


class GraphTaskService:
    def __init__(self, runner, writer, metadata_provider=None, metadata_timeout=5):
        self.runner = runner
        self.writer = writer
        self.metadata_provider = metadata_provider
        self.metadata_timeout = metadata_timeout
        self.states = {}

    def __call__(self, first, second):
        if hasattr(first, "task_id"):
            return self.start(first, second)
        return self.feedback(first, second)

    def start(self, task, data: dict):
        try:
            metadata = data.get("graph_metadata")
            if metadata is None and self.metadata_provider is not None:
                pool = ThreadPoolExecutor(max_workers=1)
                future = pool.submit(self.metadata_provider)
                try:
                    metadata = future.result(timeout=self.metadata_timeout)
                except TimeoutError as exc:
                    raise TimeoutError("工具元数据读取超时") from exc
                finally:
                    future.cancel()
                    pool.shutdown(wait=False, cancel_futures=True)
            state = self.runner.start(data["query"], metadata, thread_id=task.task_id)
        except Exception as exc:
            state = {"status": "failed", "error": f"Graph 启动失败: {exc}"}
        self.states[task.task_id] = state
        self.writer.write(task.task_id, state)
        return state

    def feedback(self, task_id: str, text: str):
        state = self.states.get(task_id)
        if state is None and hasattr(self.runner, "resume"):
            recovered = self.runner.resume(task_id)
            state = recovered if isinstance(recovered, dict) else None
        if state is None:
            raise ValueError(f"找不到 Graph 任务状态: {task_id}")
        try:
            updated = self.runner.feedback(state, text)
        except Exception as exc:
            updated = {**state, "status": "failed", "error": f"Graph 反馈处理失败: {exc}"}
        self.states[task_id] = updated
        self.writer.write(task_id, updated)
        return updated
