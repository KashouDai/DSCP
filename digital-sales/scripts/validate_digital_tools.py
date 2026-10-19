"""校验 Agent 工具注册表中的权限是否存在于 digital-sales 初始化菜单。"""

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "docs" / "digital-sales-tools.json"
MENU_SQL = ROOT / "backend" / "sql" / "digital_init.sql"


def main() -> None:
    registry = json.loads(REGISTRY.read_text(encoding="utf-8"))
    sql = MENU_SQL.read_text(encoding="utf-8")
    menu_permissions = set(re.findall(r"digital:[a-z]+:[a-z]+", sql))
    tool_permissions = {
        tool["permission"]
        for tool in registry["tools"]
        if tool.get("permission")
    }
    missing = sorted(tool_permissions - menu_permissions)
    if missing:
        raise SystemExit("工具权限不存在于菜单 SQL: " + ", ".join(missing))
    print(f"OK: {len(registry['tools'])} tools, {len(tool_permissions)} permissions")


if __name__ == "__main__":
    main()
