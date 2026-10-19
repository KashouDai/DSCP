package com.dop.digital.agent.query;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 查询对象、字段与指标白名单。所有 SQL 标识符只能来自本目录。 */
public final class AgentQueryCatalog
{
    private static final Map<String, Subject> SUBJECTS = new LinkedHashMap<>();
    private static final Map<String, Field> FIELDS = new LinkedHashMap<>();
    private static final Map<String, Metric> METRICS = new LinkedHashMap<>();

    static
    {
        SUBJECTS.put("sale_order", new Subject(
            "biz_sale_order o left join biz_customer c on c.customer_id = o.customer_id and c.del_flag = '0'",
            Set.of("order", "customer")));
        SUBJECTS.put("customer", new Subject("biz_customer c", Set.of("customer")));
        SUBJECTS.put("after_sale", new Subject(
            "biz_after_sale_ticket t inner join biz_sale_order_item i on i.item_id = t.order_item_id " +
            "inner join biz_sale_order o on o.order_id = i.order_id " +
            "left join biz_customer c on c.customer_id = o.customer_id and c.del_flag = '0'",
            Set.of("after_sale", "order", "customer")));
        SUBJECTS.put("refund", new Subject(
            "biz_refund_record r inner join biz_after_sale_ticket t on t.ticket_id = r.ticket_id " +
            "inner join biz_sale_order_item i on i.item_id = t.order_item_id " +
            "inner join biz_sale_order o on o.order_id = i.order_id " +
            "left join biz_customer c on c.customer_id = o.customer_id and c.del_flag = '0'",
            Set.of("refund", "after_sale", "order", "customer")));
        SUBJECTS.put("sku", new Subject(
            "biz_product_sku s inner join biz_product p on p.product_id = s.product_id",
            Set.of("sku", "product")));

        field("customer.id", "c.customer_id", "customerId", "客户ID", Set.of("eq", "in"));
        field("customer.name", "c.customer_name", "customerName", "客户名称", Set.of("eq", "contains", "in"));
        field("customer.phone", "c.phone", "customerPhone", "联系电话", Set.of("eq", "contains"));
        field("customer.email", "c.email", "customerEmail", "邮箱", Set.of("eq", "contains"));
        field("customer.status", "c.status", "customerStatus", "客户状态", Set.of("eq", "in"));
        field("customer.createTime", "c.create_time", "customerCreateTime", "客户创建时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));

        field("order.id", "o.order_id", "orderId", "订单ID", Set.of("eq", "in"));
        field("order.no", "o.order_no", "orderNo", "订单号", Set.of("eq", "contains", "in"));
        field("order.totalAmount", "o.total_amount", "totalAmount", "订单金额", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("order.status", "o.status", "orderStatus", "订单状态", Set.of("eq", "in"));
        field("order.createTime", "o.create_time", "createTime", "订单创建时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("order.confirmTime", "o.confirm_time", "confirmTime", "订单确认时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("order.completeTime", "o.complete_time", "completeTime", "订单完成时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("order.month", "DATE_FORMAT(o.complete_time, '%Y-%m')", "orderMonth", "订单完成月份", Set.of("eq", "in"));

        field("afterSale.no", "t.ticket_no", "ticketNo", "售后单号", Set.of("eq", "contains", "in"));
        field("afterSale.status", "t.status", "afterSaleStatus", "售后状态", Set.of("eq", "in"));
        field("afterSale.type", "t.ticket_type", "ticketType", "售后类型", Set.of("eq", "in"));
        field("afterSale.createTime", "t.create_time", "afterSaleCreateTime", "售后创建时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));

        field("refund.id", "r.refund_id", "refundId", "退款单ID", Set.of("eq", "in"));
        field("refund.amount", "r.refund_amount", "refundAmount", "退款金额", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("refund.status", "r.status", "refundStatus", "退款状态", Set.of("eq", "in"));
        field("refund.createTime", "r.create_time", "refundCreateTime", "退款创建时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("refund.handleTime", "r.handle_time", "refundHandleTime", "退款处理时间", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("refund.month", "DATE_FORMAT(r.create_time, '%Y-%m')", "refundMonth", "退款创建月份", Set.of("eq", "in"));

        field("sku.id", "s.sku_id", "skuId", "SKU ID", Set.of("eq", "in"));
        field("sku.code", "s.sku_code", "skuCode", "SKU编码", Set.of("eq", "contains", "in"));
        field("sku.name", "s.sku_name", "skuName", "SKU名称", Set.of("eq", "contains", "in"));
        field("sku.stock", "s.stock", "stock", "库存", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("sku.warnStock", "s.warn_stock", "warnStock", "库存预警值", Set.of("eq", "gt", "gte", "lt", "lte", "between"));
        field("sku.status", "s.status", "skuStatus", "SKU状态", Set.of("eq", "in"));
        field("product.name", "p.product_name", "productName", "商品名称", Set.of("eq", "contains", "in"));
        field("product.status", "p.status", "productStatus", "商品状态", Set.of("eq", "in"));

        metric("order_count", "COUNT(DISTINCT o.order_id)", "orderCount", "订单数量", Set.of("sale_order", "after_sale", "refund"));
        metric("order_amount_sum", "COALESCE(SUM(o.total_amount), 0)", "orderAmount", "订单金额合计", Set.of("sale_order"));
        metric("order_amount_avg", "COALESCE(AVG(o.total_amount), 0)", "orderAmountAvg", "平均订单金额", Set.of("sale_order"));
        metric("refund_count", "COUNT(DISTINCT r.refund_id)", "refundCount", "退款数量", Set.of("refund"));
        metric("refund_amount_sum", "COALESCE(SUM(r.refund_amount), 0)", "refundAmount", "退款金额合计", Set.of("refund"));
        metric("after_sale_count", "COUNT(DISTINCT t.ticket_id)", "afterSaleCount", "售后数量", Set.of("after_sale", "refund"));
        metric("customer_count", "COUNT(DISTINCT c.customer_id)", "customerCount", "客户数量", Set.of("customer", "sale_order", "after_sale", "refund"));
        metric("sku_count", "COUNT(DISTINCT s.sku_id)", "skuCount", "SKU数量", Set.of("sku"));
        metric("low_stock_count", "COALESCE(SUM(CASE WHEN s.warn_stock > 0 AND s.stock <= s.warn_stock THEN 1 ELSE 0 END), 0)", "lowStockCount", "低库存SKU数量", Set.of("sku"));
    }

    private AgentQueryCatalog() { }

    private static void field(String key, String expression, String alias, String label, Set<String> operators)
    {
        FIELDS.put(key, new Field(expression, alias, label, operators));
    }

    private static void metric(String key, String expression, String alias, String label, Set<String> subjects)
    {
        METRICS.put(key, new Metric(expression, alias, label, subjects));
    }

    public static Subject subject(String key) { return SUBJECTS.get(key); }
    public static Field field(String key) { return FIELDS.get(key); }
    public static Metric metric(String key) { return METRICS.get(key); }

    public record Subject(String fromClause, Set<String> namespaces) { }
    public record Field(String expression, String alias, String label, Set<String> operators) { }
    public record Metric(String expression, String alias, String label, Set<String> subjects) { }
}
