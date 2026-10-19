from workflow.schemas import ToolSelection


class ToolSelectionChain:
    def __init__(self, model):
        self._chain = model.with_structured_output(
            ToolSelection,
            method="function_calling",
        )

    def select(
        self,
        prompt: str,
        candidate_names: list[str],
    ) -> ToolSelection:
        result = self._chain.invoke(prompt)
        selection = ToolSelection.model_validate(result)

        if (
            selection.tool_name is not None
            and selection.tool_name not in candidate_names
        ):
            raise ValueError(
                f"工具 {selection.tool_name} 不在候选工具列表中"
            )

        return selection
