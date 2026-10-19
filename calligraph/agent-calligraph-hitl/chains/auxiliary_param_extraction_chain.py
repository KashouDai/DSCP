from workflow.schemas import ParameterExtraction


class AuxiliaryParamExtractionChain:
    def __init__(self, model):
        self._chain = model.with_structured_output(ParameterExtraction, method="function_calling")

    def extract(self, prompt: str, allowed_names: list[str], required_names: list[str] | None = None) -> ParameterExtraction:
        extraction = ParameterExtraction.model_validate(self._chain.invoke(prompt))
        unknown = set(extraction.parameters) - set(allowed_names)
        if unknown:
            raise ValueError(f"辅助工具检测到未知参数: {', '.join(sorted(unknown))}")
        missing = set(required_names or []) - set(extraction.parameters)
        if missing:
            raise ValueError(f"辅助工具缺少必填参数: {', '.join(sorted(missing))}")
        return extraction
