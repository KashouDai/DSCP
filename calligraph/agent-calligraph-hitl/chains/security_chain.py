from workflow.schemas import SecurityDecision
from pydantic import ValidationError


class SecurityChain:
    def __init__(self, model):
        self._chain = model.with_structured_output(
            SecurityDecision,
            method="function_calling",
        )

    def check(self, prompt: str) -> SecurityDecision:
        result = self._chain.invoke(prompt)
        try:
            return SecurityDecision.model_validate(result)
        except ValidationError:
            return SecurityDecision(
                should_block=True,
                reason="安全检查结果不完整，已阻止工具调用",
            )
