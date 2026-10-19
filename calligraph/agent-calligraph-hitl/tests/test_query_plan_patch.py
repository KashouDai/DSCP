import unittest

from apis.api_planning_hub import ApiPlanningHub


class QueryPlanPatchTest(unittest.TestCase):
    def setUp(self):
        self.hub = object.__new__(ApiPlanningHub)
        self.order_plan = {
            "subject": "sale_order",
            "detail": True,
            "fields": [
                "order.no", "customer.name", "order.totalAmount",
                "order.status", "order.createTime",
            ],
            "filters": [
                {"field": "customer.name", "operator": "eq", "value": "张志勇"},
                {"field": "order.createTime", "operator": "between", "value": [
                    "2026-01-01 00:00:00", "2027-01-01 00:00:00",
                ]},
            ],
            "metrics": [],
            "limit": 20,
        }

    def test_switch_to_refund_replaces_fields_and_preserves_customer(self):
        plan = self.hub.patch_read_query_plan("他的退款金额呢？今年", self.order_plan)

        self.assertEqual(plan["subject"], "refund")
        self.assertIn("refund.amount", plan["fields"])
        self.assertNotIn("order.totalAmount", plan["fields"])
        self.assertEqual(plan["filters"][0]["field"], "customer.name")

    def test_change_customer_only_replaces_customer_filter(self):
        refund_plan = self.hub.patch_read_query_plan("他的退款金额呢？今年", self.order_plan)
        plan = self.hub.patch_read_query_plan("换成李娜", refund_plan)

        self.assertEqual(plan["subject"], "refund")
        self.assertEqual(
            next(item["value"] for item in plan["filters"] if item["field"] == "customer.name"),
            "李娜",
        )
        self.assertIn("refund.amount", plan["fields"])

    def test_refund_monthly_statistics_keep_refund_subject_and_amount(self):
        refund_plan = self.hub.patch_read_query_plan("他的退款金额呢？今年", self.order_plan)
        plan = self.hub.patch_read_query_plan("按月统计退款金额", refund_plan)

        self.assertEqual(plan["subject"], "refund")
        self.assertEqual(plan["groupBy"], ["refund.month"])
        self.assertEqual(plan["metrics"], ["refund_count", "refund_amount_sum"])

    def test_monthly_switch_restores_order_aggregation(self):
        refund_plan = self.hub.patch_read_query_plan("他的退款金额呢？今年", self.order_plan)
        customer_plan = self.hub.patch_read_query_plan("换成李娜", refund_plan)
        plan = self.hub.patch_read_query_plan("再按月统计", customer_plan)

        self.assertEqual(plan["subject"], "sale_order")
        self.assertFalse(plan["detail"])
        self.assertEqual(plan["fields"], [])
        self.assertEqual(plan["metrics"], ["order_count", "order_amount_sum"])
        self.assertEqual(plan["groupBy"], ["order.month"])
        self.assertIn({"field": "order.createTime", "operator": "between", "value": [
            "2026-01-01 00:00:00", "2027-01-01 00:00:00",
        ]}, plan["filters"])
        self.assertIn({"field": "customer.name", "operator": "eq", "value": "李娜"}, plan["filters"])

    def test_cross_context_chain_does_not_leak_subject_or_customer(self):
        customer_plan = self.hub.patch_read_query_plan("换成张伟", self.order_plan)
        refund_plan = self.hub.patch_read_query_plan("他的退款金额呢？今年", customer_plan)
        refund_monthly_plan = self.hub.patch_read_query_plan("按月统计退款数量和退款金额", refund_plan)
        order_monthly_plan = self.hub.patch_read_query_plan("再按月统计", refund_monthly_plan)

        self.assertEqual(customer_plan["subject"], "sale_order")
        self.assertEqual(refund_plan["subject"], "refund")
        self.assertEqual(refund_monthly_plan["subject"], "refund")
        self.assertEqual(refund_monthly_plan["metrics"], ["refund_count", "refund_amount_sum"])
        self.assertEqual(order_monthly_plan["subject"], "sale_order")
        self.assertEqual(order_monthly_plan["metrics"], ["order_count", "order_amount_sum"])
        self.assertEqual(order_monthly_plan["groupBy"], ["order.month"])
        self.assertIn({"field": "customer.name", "operator": "eq", "value": "张伟"}, order_monthly_plan["filters"])
        self.assertNotIn("refund.month", order_monthly_plan["groupBy"])

    def test_explicit_new_order_query_does_not_patch_previous_refund_plan(self):
        refund_plan = self.hub.patch_read_query_plan("按月统计退款金额", self.order_plan)

        self.assertIsNone(self.hub.patch_read_query_plan("帮我查张志勇的订单", refund_plan))

    def test_global_refund_monthly_amount_does_not_require_customer(self):
        plan = self.hub._match_grouped_read_query_plan("按月统计退款金额")

        self.assertEqual(plan["subject"], "refund")
        self.assertEqual(plan["metrics"], ["refund_amount_sum"])
        self.assertEqual(plan["groupBy"], ["refund.month"])
        self.assertEqual(plan["filters"], [])

    def test_global_refund_monthly_count_and_amount_keep_both_metrics(self):
        plan = self.hub._match_grouped_read_query_plan("按月统计退款数量和退款金额")

        self.assertEqual(plan["subject"], "refund")
        self.assertEqual(plan["metrics"], ["refund_count", "refund_amount_sum"])
        self.assertEqual(plan["groupBy"], ["refund.month"])

    def test_refund_monthly_summary_aggregates_all_rows(self):
        summary = self.hub._summarize_agent_query({
            "rows": [
                {"refundMonth": "2026-09", "refundCount": 1, "refundAmount": 239},
                {"refundMonth": "2026-10", "refundCount": 3, "refundAmount": 24497},
            ],
            "summary": {"refundAmount": 239},
        })
        self.assertIn("退款金额合计 24736 元", summary)
        self.assertIn("共 4 条", summary)

    def test_refund_monthly_amount_summary_does_not_invent_count(self):
        summary = self.hub._summarize_agent_query({
            "rows": [
                {"refundMonth": "2026-09", "refundAmount": 239},
                {"refundMonth": "2026-10", "refundAmount": 24497},
            ],
            "summary": {"refundAmount": 239},
        })
        self.assertNotIn("0 条", summary)
        self.assertIn("退款金额合计 24736 元", summary)


class QueryPlanValidationTest(unittest.TestCase):
    class FakeLLM:
        def __init__(self, response):
            self.responses = response if isinstance(response, list) else [response]
            self.calls = 0

        def chat_completions(self, *args, **kwargs):
            response = self.responses[min(self.calls, len(self.responses) - 1)]
            self.calls += 1
            if isinstance(response, Exception):
                raise response
            return response

    def setUp(self):
        self.hub = object.__new__(ApiPlanningHub)
        self.hub.model_api_key = "test-key"
        self.hub.model = "test-model"
        self.hub.top_p = 1

    def test_invalid_model_plan_is_rejected(self):
        self.hub.llm = self.FakeLLM('{"subject":"sale_order","fields":["order.secret"]}')
        self.assertIsNone(self.hub._match_model_read_query_plan("查询订单"))

    def test_limit_and_unknown_metric_are_rejected(self):
        self.hub.llm = self.FakeLLM(
            '{"subject":"sale_order","metrics":["unknown_metric"],"limit":101}'
        )
        self.assertIsNone(self.hub._match_model_read_query_plan("统计订单"))

    def test_invalid_first_output_is_repaired_once(self):
        self.hub.llm = self.FakeLLM([
            '{"subject":"sale_order","fields":["order.secret"]}',
            '{"subject":"sale_order","detail":false,"fields":[],"filters":[],"metrics":["order_count"],"groupBy":["customer.name"],"limit":20}',
        ])
        plan = self.hub._match_model_read_query_plan("统计各客户订单数量")

        self.assertEqual(plan["metrics"], ["order_count"])
        self.assertEqual(self.hub.llm.calls, 2)

    def test_model_detail_fields_without_detail_flag_are_normalized(self):
        self.hub.llm = self.FakeLLM(
            '{"subject":"customer","fields":["customer.id","customer.name"],"filters":[],"metrics":[],"limit":20}'
        )
        plan = self.hub._match_model_read_query_plan("查询客户信息")

        self.assertTrue(plan["detail"])
        self.assertEqual(plan["fields"], ["customer.id", "customer.name"])

    def test_customer_detail_without_identity_requires_clarification(self):
        self.hub.llm = self.FakeLLM(
            '{"subject":"customer","fields":["customer.id","customer.name"],"filters":[],"metrics":[],"limit":20}'
        )
        plan = self.hub._match_model_read_query_plan("查询客户信息")

        self.assertEqual(plan["missingFields"], ["customer.identity"])

    def test_pronoun_customer_query_requires_clarification(self):
        self.hub.llm = self.FakeLLM(
            '{"subject":"sale_order","detail":true,"fields":["order.no"],"filters":[],"metrics":[],"limit":20}'
        )
        plan = self.hub._match_read_query_plan("查询他的订单")

        self.assertEqual(plan["missingFields"], ["customer.identity"])
        self.assertEqual(plan["filters"], [])

    def test_model_timeout_returns_none_for_deterministic_fallback(self):
        self.hub.llm = self.FakeLLM(TimeoutError("timeout"))
        self.assertIsNone(self.hub._match_model_read_query_plan("统计订单"))

    def test_model_patch_accepts_safe_general_change(self):
        previous = {
            "subject": "sale_order", "detail": False, "fields": [], "filters": [],
            "metrics": ["order_count", "order_amount_sum"], "groupBy": ["customer.name"],
            "orderBy": [{"field": "order_amount_sum", "direction": "desc"}], "limit": 20,
        }
        self.hub.llm = self.FakeLLM(
            '{"subject":"sale_order","detail":false,"fields":[],"filters":[{"field":"order.status","operator":"eq","value":"COMPLETED"}],"metrics":["order_count","order_amount_sum"],"groupBy":["customer.name"],"orderBy":[{"field":"order_amount_sum","direction":"desc"}],"limit":5}'
        )
        plan = self.hub.patch_model_read_query_plan("只看前5名已完成订单", previous)

        self.assertEqual(plan["limit"], 5)
        self.assertEqual(plan["filters"][0]["field"], "order.status")

    def test_model_patch_rejects_unsafe_field(self):
        previous = {"subject": "sale_order", "detail": False, "fields": [], "filters": [], "metrics": ["order_count"], "limit": 20}
        self.hub.llm = self.FakeLLM(
            '{"subject":"sale_order","detail":false,"fields":["order.secret"],"filters":[],"metrics":["order_count"],"limit":20}'
        )
        self.assertIsNone(self.hub.patch_model_read_query_plan("只看前5名", previous))

    def test_result_summary_uses_facts_for_aggregate(self):
        self.hub.llm = self.FakeLLM("张三销售额最高，订单金额合计 100 元。")
        payload = {
            "rows": [{"customerName": "张三", "orderCount": 2, "orderAmount": 100}],
            "summary": {}, "queryMeta": {"detail": False},
        }
        self.assertEqual(
            self.hub._model_summarize_agent_query(payload, "固定摘要"),
            "张三销售额最高，订单金额合计 100 元。",
        )

    def test_result_summary_keeps_deterministic_text_for_detail(self):
        self.hub.llm = self.FakeLLM("不应被使用")
        payload = {
            "rows": [{"orderNo": "SO1"}], "summary": {},
            "queryMeta": {"detail": True},
        }
        self.assertEqual(self.hub._model_summarize_agent_query(payload, "明细固定摘要"), "明细固定摘要")


class QuerySafetyTest(unittest.TestCase):
    class FakeTaskManager:
        def __init__(self):
            self.last_update = None

        def update_task_recorder(self, *args, **kwargs):
            self.last_update = (args, kwargs)

    def setUp(self):
        self.hub = object.__new__(ApiPlanningHub)
        self.hub.task_manager = self.FakeTaskManager()
        self.hub.model_api_key = None

    def assert_rejected(self, query, expected_text):
        self.hub.apis_planning(query, "test-task")
        args, _ = self.hub.task_manager.last_update
        self.assertIn(expected_text, args[2])

    def test_rejects_sql(self):
        self.assert_rejected("select * from biz_customer", "不支持原始SQL")

    def test_rejects_write_operation(self):
        self.assert_rejected("删除客户张三", "不支持原始SQL")

    def test_rejects_unknown_field(self):
        self.assert_rejected("查询字段 secret_field", "无法识别查询字段")

    def test_rejects_more_than_100_rows(self):
        self.assert_rejected("查询订单返回101条", "最多返回100条")


if __name__ == "__main__":
    unittest.main()
