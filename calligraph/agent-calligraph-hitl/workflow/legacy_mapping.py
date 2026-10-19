def to_legacy_graph(state: dict) -> dict:
    return {
        "nodes": state.get("nodes", []),
        "edges": state.get("edges", []),
        "title": "异常调用链" if state.get("status") in {"failed", "blocked"} else "正常调用链",
    }
