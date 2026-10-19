from workflow.tool_metadata import build_tool_metadata


class ToolCatalog:
    def __init__(self, loader):
        self.loader = loader
        self._metadata = None

    def load(self):
        if self._metadata is None:
            self._metadata = build_tool_metadata(self.loader())
        return self._metadata

    def clear(self):
        self._metadata = None
