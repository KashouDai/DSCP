def build_tool_metadata(tools: list) -> dict:
    candidates = []
    allowed = []
    required = []
    for tool in tools:
        if isinstance(tool, dict):
            name = tool.get("operation_id") or tool.get("operationId") or tool.get("name")
            parameters = tool.get("request_body") or tool.get("params") or []
        else:
            name = getattr(tool, "operation_id", None) or getattr(tool, "operationId", None) or getattr(tool, "name", None)
            parameters = getattr(tool, "request_body", []) or []
        if name:
            candidates.append(name)
        for parameter in parameters:
            if isinstance(parameter, dict):
                parameter_name = parameter.get("name")
                is_required = parameter.get("required", False)
            else:
                parameter_name = getattr(parameter, "name", None)
                is_required = getattr(parameter, "required", False)
            if parameter_name:
                allowed.append(parameter_name)
                if is_required:
                    required.append(parameter_name)
    return {
        "candidate_tools": list(dict.fromkeys(candidates)),
        "allowed_parameters": list(dict.fromkeys(allowed)),
        "required_parameters": list(dict.fromkeys(required)),
    }
