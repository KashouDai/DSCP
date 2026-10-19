from workflow.auxiliary_registry import get_auxiliary_mapping


def prepare_auxiliary_action(main_tool: str, parameters: dict) -> dict:
    mapping = get_auxiliary_mapping(main_tool)
    if mapping is None:
        return {"status": "blocked", "error": "未登记辅助工具映射"}
    auxiliary_parameters = {}
    for auxiliary_name, main_name in mapping["parameter_mapping"].items():
        if main_name not in parameters:
            return {"status": "blocked", "error": f"缺少辅助参数: {main_name}"}
        auxiliary_parameters[auxiliary_name] = parameters[main_name]
    return {"status": "need_confirmation", "main_tool": main_tool, "auxiliary_tool": mapping["auxiliary_tool"], "parameters": auxiliary_parameters}
