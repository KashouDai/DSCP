"""Normalize Java Gateway responses before they enter Agent planning context."""

import json


SUCCESS_CODE = 200


def normalize_response(response):
    """Return a stable business result and never slice raw JSON by characters."""
    if response.status_code < 200 or response.status_code >= 300:
        return False, {"success": False, "errorMessage": f"HTTP {response.status_code}"}

    try:
        payload = response.json()
    except ValueError:
        return True, {"success": True, "data": response.text}

    if not isinstance(payload, dict):
        return True, {"success": True, "data": payload}

    if payload.get("code") not in (None, SUCCESS_CODE):
        return False, {
            "success": False,
            "errorCode": payload.get("code"),
            "errorMessage": payload.get("msg") or payload.get("message") or "业务调用失败",
        }

    data = payload.get("data", payload)
    if isinstance(data, dict) and data.get("success") is False:
        return False, {
            "success": False,
            "errorCode": data.get("errorCode", "BUSINESS_ERROR"),
            "errorMessage": data.get("errorMessage") or "业务操作失败",
            "data": data.get("data"),
        }
    if isinstance(data, dict) and data.get("code") not in (None, SUCCESS_CODE):
        return False, {
            "success": False,
            "errorCode": data.get("code"),
            "errorMessage": data.get("msg") or data.get("message") or "业务调用失败",
        }

    if isinstance(data, dict) and "data" in data and set(data).issubset({"code", "msg", "data"}):
        data = data["data"]

    result = {"success": True, "data": data}
    if isinstance(data, dict) and "rows" in data:
        rows = data.get("rows") or []
        total = data.get("total", len(rows))
        result.update({
            "total": total,
            "returned": len(rows),
            "complete": len(rows) >= total,
            "rows": rows,
        })
    return True, result


def result_text(result):
    return json.dumps(result, ensure_ascii=False, separators=(",", ":"))
