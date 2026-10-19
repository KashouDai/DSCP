from workflow.schemas import SummaryResult


class SummaryChain:
    def __init__(self, model):
        self._chain = model.with_structured_output(
            SummaryResult,
            method="function_calling",
        )

    def summarize(self, prompt: str) -> SummaryResult:
        return SummaryResult.model_validate(self._chain.invoke(prompt))
