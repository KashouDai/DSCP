def recover_state(graph, thread_id: str):
    """Read the latest checkpoint state for a LangGraph thread."""
    snapshot = graph.get_state({"configurable": {"thread_id": thread_id}})
    if snapshot is None:
        return None
    return snapshot.values
