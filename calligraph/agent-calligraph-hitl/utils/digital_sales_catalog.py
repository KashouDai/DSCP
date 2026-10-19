import json
import os
from entity import Parameter, Tool


class DigitalSalesCatalog:
    """只读加载 digital-sales 的业务工具映射，业务执行仍走 Java Gateway。"""

    def __init__(self, path=None):
        self.path = path or os.getenv(
            "DIGITAL_SALES_TOOLS_FILE", "/app/catalog/digital-sales-tools.json"
        )
        self.data = self._load()

    def _load(self):
        with open(self.path, encoding="utf-8") as file:
            data = json.load(file)
        if not isinstance(data.get("tools"), list):
            raise ValueError("digital-sales 工具注册表缺少 tools 数组")
        for tool in data["tools"]:
            for field in ("name", "operation", "method", "path", "permission"):
                if not tool.get(field):
                    raise ValueError(f"工具注册表字段缺失: {field}")
        return data

    def summary(self):
        tools = self.data["tools"]
        return {
            "schemaVersion": self.data.get("schemaVersion"),
            "status": self.data.get("status"),
            "toolCount": len(tools),
            "writeToolCount": sum(tool["operation"] == "write" for tool in tools),
        }

    def to_tools(self):
        tools = []
        for index, item in enumerate(self.data["tools"], start=1):
            parameters = [Parameter(
                name=parameter["name"],
                required=parameter.get("required", False),
                type=parameter.get("type", "string"),
                format=parameter.get("format", "string"),
                description=parameter.get("description", ""),
                enum=parameter.get("enum", []),
                in_=parameter.get("in") or parameter.get("in_", "requestBody"),
                source=parameter.get("source", "userInput"),
            ) for parameter in item.get("parameters", [])]
            tools.append(Tool(
                tool_id=index,
                isValidate=item["operation"] == "write",
                operationId=item["name"],
                name_for_human=item["name"],
                name_for_model=item["name"],
                description=item.get("description") or item["controller"],
                api_url="",
                path=item["path"],
                method=item["method"],
                request_body=parameters,
            ))
        return tools
