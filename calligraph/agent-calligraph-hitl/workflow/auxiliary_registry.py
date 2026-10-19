"""Explicit auxiliary-tool mappings discovered from the existing tool catalog."""

AUXILIARY_MAPPINGS = {
    "getOrdersBySupplierId": {
        "auxiliary_tool": "getSupplierByName",
        "field_mapping": {"supplierId": "supplierId"},
        "parameter_mapping": {"name": "name"},
    },
}


def get_auxiliary_mapping(main_tool: str) -> dict | None:
    return AUXILIARY_MAPPINGS.get(main_tool)
