from workflow.schemas import HumanIntent


class HumanIntentChain:
    def __init__(self, model):
        self._chain = model.with_structured_output(
            HumanIntent,
            method="function_calling",
        )

    def classify(self, prompt: str) -> HumanIntent:
        return HumanIntent.model_validate(self._chain.invoke(prompt))
