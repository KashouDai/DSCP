from workflow.schemas import NextTaskDecision, TaskDecision


class TaskChain:
    def __init__(self, model):
        self._root_chain = model.with_structured_output(
            TaskDecision,
            method="function_calling",
        )
        
        self._next_task_chain = model.with_structured_output(
              NextTaskDecision,
              method="function_calling",
        )

    def classify(self, prompt: str) -> TaskDecision:
        result = self._root_chain.invoke(prompt)
        return TaskDecision.model_validate(result)
        
    def decide_next_task(self, prompt: str) -> NextTaskDecision:
        result = self._next_task_chain.invoke(prompt)
        return NextTaskDecision.model_validate(result)
