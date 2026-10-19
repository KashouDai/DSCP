import os


def is_langgraph_enabled(value: str | None = None) -> bool:
    """Read the migration switch; invalid values safely fall back to disabled."""
    raw = os.getenv("USE_LANGGRAPH", "0") if value is None else value
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def select_workflow(value: str | None = None) -> str:
    return "langgraph" if is_langgraph_enabled(value) else "legacy"
