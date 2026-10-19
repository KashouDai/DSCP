from workflow.schemas import ParameterExtraction


class ParamExtractionChain:
    def __init__(self, model):
        self._chain = model.with_structured_output(
            ParameterExtraction,
            method="function_calling",
        )

    def extract(
        self,
        prompt: str,
        allowed_names: list[str],
        required_names: list[str] | None = None,
        validate_required: bool = True,
    ) -> ParameterExtraction:
        result = self._chain.invoke(prompt)
        extraction = ParameterExtraction.model_validate(result)

        unknown_names = set(extraction.parameters) - set(allowed_names)
        if unknown_names:
            names = ", ".join(sorted(unknown_names))
            raise ValueError(f"检测到未知参数: {names}")

        missing_names = set(required_names or []) - set(extraction.parameters)
        if validate_required and missing_names:
            names = ", ".join(sorted(missing_names))
            raise ValueError(f"缺少必填参数: {names}")

        return extraction
