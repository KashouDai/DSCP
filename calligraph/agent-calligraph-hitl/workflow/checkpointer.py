import sqlite3

from langgraph.checkpoint.sqlite import SqliteSaver


def create_sqlite_checkpointer(path: str):
    """Create a persistent SQLite checkpointer for local or single-instance deployment."""
    connection = sqlite3.connect(path, check_same_thread=False)
    return SqliteSaver(connection)
