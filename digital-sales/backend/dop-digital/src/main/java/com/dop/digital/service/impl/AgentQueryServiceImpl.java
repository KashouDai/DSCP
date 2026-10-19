package com.dop.digital.service.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dop.digital.agent.query.AgentQueryCatalog;
import com.dop.digital.agent.query.AgentQueryException;
import com.dop.digital.agent.query.domain.AgentQueryPlan;
import com.dop.digital.agent.query.domain.AgentQueryResult;
import com.dop.digital.agent.query.domain.QueryFilter;
import com.dop.digital.agent.query.domain.QueryOrder;
import com.dop.digital.service.IAgentQueryService;

/** 受白名单约束的只读查询执行器。 */
@Service
public class AgentQueryServiceImpl implements IAgentQueryService
{
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_FILTERS = 10;
    private static final int MAX_GROUPS = 2;
    private static final int MAX_ORDERS = 3;

    @Autowired
    private DataSource dataSource;

    @Override
    public AgentQueryResult execute(AgentQueryPlan plan)
    {
        validate(plan);
        long started = System.currentTimeMillis();
        List<Map<String, Object>> summaryRows = plan.getMetrics().isEmpty()
                ? List.of() : query(compile(plan, false));
        List<Map<String, Object>> detailRows = Boolean.TRUE.equals(plan.getDetail())
                ? query(compile(plan, true)) : List.of();

        AgentQueryResult result = new AgentQueryResult();
        result.setSummary(summaryRows.isEmpty() ? new LinkedHashMap<>() : summaryRows.get(0));
        result.setRows(Boolean.TRUE.equals(plan.getDetail()) ? detailRows : summaryRows);
        result.setColumns(columns(plan, Boolean.TRUE.equals(plan.getDetail())));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("total", result.getRows().size());
        meta.put("detail", Boolean.TRUE.equals(plan.getDetail()));
        meta.put("limited", Boolean.TRUE.equals(plan.getDetail()) && detailRows.size() >= normalizedLimit(plan));
        meta.put("executionTimeMs", System.currentTimeMillis() - started);
        meta.put("executedAt", Instant.now().toString());
        result.setQueryMeta(meta);
        return result;
    }

    private void validate(AgentQueryPlan plan)
    {
        if (plan == null || isBlank(plan.getSubject()) || AgentQueryCatalog.subject(plan.getSubject()) == null)
        {
            throw new AgentQueryException("FIELD_NOT_ALLOWED: 不支持的查询对象");
        }
        if (plan.getFilters().size() > MAX_FILTERS || plan.getGroupBy().size() > MAX_GROUPS || plan.getOrderBy().size() > MAX_ORDERS)
        {
            throw new AgentQueryException("QUERY_TOO_COMPLEX: 查询条件、分组或排序数量超过限制");
        }
        if (!Boolean.TRUE.equals(plan.getDetail()) && plan.getMetrics().isEmpty())
        {
            throw new AgentQueryException("INVALID_PARAMETER: 查询必须指定明细字段或统计指标");
        }
        for (String field : plan.getFields()) validateField(plan, field, "FIELD_NOT_ALLOWED");
        for (String field : plan.getGroupBy()) validateField(plan, field, "FIELD_NOT_ALLOWED");
        for (QueryOrder order : plan.getOrderBy())
        {
            validateOrderField(plan, order.getField());
            if (!"asc".equalsIgnoreCase(order.getDirection()) && !"desc".equalsIgnoreCase(order.getDirection()))
            {
                throw new AgentQueryException("INVALID_PARAMETER: 排序方向仅支持 asc 或 desc");
            }
        }
        for (QueryFilter filter : plan.getFilters())
        {
            validateField(plan, filter.getField(), "FIELD_NOT_ALLOWED");
            AgentQueryCatalog.Field field = AgentQueryCatalog.field(filter.getField());
            if (isBlank(filter.getOperator()) || !field.operators().contains(filter.getOperator()))
            {
                throw new AgentQueryException("INVALID_PARAMETER: 字段不支持该筛选操作");
            }
            if (!"is_null".equals(filter.getOperator()) && !"not_null".equals(filter.getOperator()) && filter.getValue() == null)
            {
                throw new AgentQueryException("MISSING_PARAMETER: 筛选条件缺少值");
            }
        }
        for (String metric : plan.getMetrics())
        {
            AgentQueryCatalog.Metric definition = AgentQueryCatalog.metric(metric);
            if (definition == null || !definition.subjects().contains(plan.getSubject()))
            {
                throw new AgentQueryException("METRIC_NOT_ALLOWED: 不支持的统计指标");
            }
        }
    }

    private void validateField(AgentQueryPlan plan, String field, String errorCode)
    {
        AgentQueryCatalog.Field definition = AgentQueryCatalog.field(field);
        if (definition == null || !fieldMatchesSubject(plan.getSubject(), field))
        {
            throw new AgentQueryException(errorCode + ": 不支持的查询字段");
        }
    }

    private void validateOrderField(AgentQueryPlan plan, String key)
    {
        AgentQueryCatalog.Field field = AgentQueryCatalog.field(key);
        if (field != null && fieldMatchesSubject(plan.getSubject(), key)) return;
        AgentQueryCatalog.Metric metric = AgentQueryCatalog.metric(key);
        if (metric == null || !metric.subjects().contains(plan.getSubject()))
        {
            throw new AgentQueryException("FIELD_NOT_ALLOWED: 不支持的排序字段");
        }
    }

    private boolean fieldMatchesSubject(String subject, String field)
    {
        String namespace = field.substring(0, field.indexOf('.'));
        return AgentQueryCatalog.subject(subject).namespaces().contains(namespace);
    }

    private CompiledQuery compile(AgentQueryPlan plan, boolean detailQuery)
    {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select ");
        List<String> selectItems = new ArrayList<>();
        if (detailQuery)
        {
            List<String> selectedFields = plan.getFields().isEmpty() ? defaultFields(plan.getSubject()) : plan.getFields();
            for (String field : selectedFields)
            {
                AgentQueryCatalog.Field definition = AgentQueryCatalog.field(field);
                selectItems.add(definition.expression() + " as " + definition.alias());
            }
        }
        else
        {
            for (String group : plan.getGroupBy())
            {
                AgentQueryCatalog.Field definition = AgentQueryCatalog.field(group);
                selectItems.add(definition.expression() + " as " + definition.alias());
            }
            for (String metric : plan.getMetrics())
            {
                AgentQueryCatalog.Metric definition = AgentQueryCatalog.metric(metric);
                selectItems.add(definition.expression() + " as " + definition.alias());
            }
        }
        if (selectItems.isEmpty())
        {
            throw new AgentQueryException("INVALID_PARAMETER: 没有可查询字段");
        }
        sql.append(String.join(", ", selectItems));
        sql.append(" from ").append(AgentQueryCatalog.subject(plan.getSubject()).fromClause());
        appendWhere(sql, parameters, plan);
        if (!detailQuery && !plan.getGroupBy().isEmpty())
        {
            sql.append(" group by ");
            sql.append(String.join(", ", plan.getGroupBy().stream().map(key -> AgentQueryCatalog.field(key).expression()).toList()));
        }
        appendOrderBy(sql, plan, detailQuery);
        if (detailQuery)
        {
            sql.append(" limit ?");
            parameters.add(normalizedLimit(plan));
        }
        return new CompiledQuery(sql.toString(), parameters);
    }

    private void appendWhere(StringBuilder sql, List<Object> parameters, AgentQueryPlan plan)
    {
        List<String> predicates = new ArrayList<>();
        for (QueryFilter filter : plan.getFilters())
        {
            AgentQueryCatalog.Field field = AgentQueryCatalog.field(filter.getField());
            switch (filter.getOperator())
            {
                case "eq" -> addPredicate(predicates, parameters, field.expression() + " = ?", filter.getValue());
                case "ne" -> addPredicate(predicates, parameters, field.expression() + " <> ?", filter.getValue());
                case "gt" -> addPredicate(predicates, parameters, field.expression() + " > ?", filter.getValue());
                case "gte" -> addPredicate(predicates, parameters, field.expression() + " >= ?", filter.getValue());
                case "lt" -> addPredicate(predicates, parameters, field.expression() + " < ?", filter.getValue());
                case "lte" -> addPredicate(predicates, parameters, field.expression() + " <= ?", filter.getValue());
                case "contains" -> addPredicate(predicates, parameters, field.expression() + " like ?", "%" + filter.getValue() + "%");
                case "between" -> appendBetween(predicates, parameters, field.expression(), filter.getValue());
                case "in" -> appendIn(predicates, parameters, field.expression(), filter.getValue());
                case "is_null" -> predicates.add(field.expression() + " is null");
                case "not_null" -> predicates.add(field.expression() + " is not null");
                default -> throw new AgentQueryException("INVALID_PARAMETER: 不支持的筛选操作");
            }
        }
        if ("customer".equals(plan.getSubject())) predicates.add("c.del_flag = '0'");
        if ("sku".equals(plan.getSubject()))
        {
            predicates.add("s.del_flag = '0'");
            predicates.add("p.del_flag = '0'");
        }
        if (containsOrderMetric(plan) && plan.getFilters().stream()
                .noneMatch(filter -> "order.status".equals(filter.getField())))
        {
            predicates.add("o.status <> 'CANCELLED'");
        }
        if (!predicates.isEmpty()) sql.append(" where ").append(String.join(" and ", predicates));
    }

    private boolean containsOrderMetric(AgentQueryPlan plan)
    {
        return plan.getMetrics().stream().anyMatch(metric -> metric.startsWith("order_"));
    }

    private void addPredicate(List<String> predicates, List<Object> parameters, String sql, Object value)
    {
        predicates.add(sql);
        parameters.add(value);
    }

    private void appendBetween(List<String> predicates, List<Object> parameters, String field, Object value)
    {
        if (!(value instanceof List<?> values) || values.size() != 2)
        {
            throw new AgentQueryException("INVALID_PARAMETER: between 条件必须提供两个值");
        }
        predicates.add(field + " between ? and ?");
        parameters.add(values.get(0));
        parameters.add(values.get(1));
    }

    private void appendIn(List<String> predicates, List<Object> parameters, String field, Object value)
    {
        if (!(value instanceof List<?> values) || values.isEmpty())
        {
            throw new AgentQueryException("INVALID_PARAMETER: in 条件必须提供非空数组");
        }
        predicates.add(field + " in (" + String.join(", ", java.util.Collections.nCopies(values.size(), "?")) + ")");
        parameters.addAll(values);
    }

    private void appendOrderBy(StringBuilder sql, AgentQueryPlan plan, boolean detailQuery)
    {
        if (plan.getOrderBy().isEmpty())
        {
            if (detailQuery && "sale_order".equals(plan.getSubject())) sql.append(" order by o.order_id desc");
            return;
        }
        List<String> orders = new ArrayList<>();
        for (QueryOrder order : plan.getOrderBy())
        {
            AgentQueryCatalog.Field field = AgentQueryCatalog.field(order.getField());
            String expression = field == null
                    ? AgentQueryCatalog.metric(order.getField()).expression() : field.expression();
            orders.add(expression + " " + order.getDirection().toUpperCase());
        }
        sql.append(" order by ").append(String.join(", ", orders));
    }

    private List<String> defaultFields(String subject)
    {
        return switch (subject)
        {
            case "sale_order" -> List.of("order.no", "customer.name", "order.totalAmount", "order.status", "order.createTime");
            case "customer" -> List.of("customer.id", "customer.name", "customer.phone", "customer.email", "customer.status");
            case "after_sale" -> List.of("afterSale.no", "customer.name", "order.no", "afterSale.status", "afterSale.createTime");
            case "refund" -> List.of("refund.id", "customer.name", "order.no", "refund.amount", "refund.status", "refund.createTime");
            case "sku" -> List.of("sku.code", "sku.name", "product.name", "sku.stock", "sku.warnStock", "sku.status");
            default -> List.of();
        };
    }

    private int normalizedLimit(AgentQueryPlan plan)
    {
        int value = plan.getLimit() == null ? DEFAULT_LIMIT : plan.getLimit();
        return Math.max(1, Math.min(MAX_LIMIT, value));
    }

    private List<Map<String, Object>> query(CompiledQuery compiled)
    {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(compiled.sql()))
        {
            statement.setQueryTimeout(5);
            for (int index = 0; index < compiled.parameters().size(); index++) statement.setObject(index + 1, compiled.parameters().get(index));
            try (ResultSet resultSet = statement.executeQuery())
            {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData metadata = resultSet.getMetaData();
                while (resultSet.next())
                {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= metadata.getColumnCount(); column++) row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
                    rows.add(row);
                }
                return rows;
            }
        }
        catch (SQLException exception)
        {
            throw new AgentQueryException("SYSTEM_ERROR: 查询执行失败");
        }
    }

    private List<Map<String, String>> columns(AgentQueryPlan plan, boolean detail)
    {
        List<Map<String, String>> columns = new ArrayList<>();
        List<String> keys = detail ? (plan.getFields().isEmpty() ? defaultFields(plan.getSubject()) : plan.getFields()) : plan.getGroupBy();
        for (String key : keys)
        {
            AgentQueryCatalog.Field field = AgentQueryCatalog.field(key);
            columns.add(Map.of("field", field.alias(), "label", field.label()));
        }
        if (!detail)
        {
            for (String key : plan.getMetrics())
            {
                AgentQueryCatalog.Metric metric = AgentQueryCatalog.metric(key);
                columns.add(Map.of("field", metric.alias(), "label", metric.label()));
            }
        }
        return columns;
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private record CompiledQuery(String sql, List<Object> parameters) { }
}
