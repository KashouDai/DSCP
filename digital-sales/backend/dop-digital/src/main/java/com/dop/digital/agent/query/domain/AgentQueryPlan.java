package com.dop.digital.agent.query.domain;

import java.util.ArrayList;
import java.util.List;

/** 只读自然语言查询计划。 */
public class AgentQueryPlan
{
    private String subject;
    private Boolean detail = false;
    private List<String> fields = new ArrayList<>();
    private List<QueryFilter> filters = new ArrayList<>();
    private List<String> metrics = new ArrayList<>();
    private List<String> groupBy = new ArrayList<>();
    private List<QueryOrder> orderBy = new ArrayList<>();
    private Integer limit = 20;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Boolean getDetail() { return detail; }
    public void setDetail(Boolean detail) { this.detail = detail; }
    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields == null ? new ArrayList<>() : fields; }
    public List<QueryFilter> getFilters() { return filters; }
    public void setFilters(List<QueryFilter> filters) { this.filters = filters == null ? new ArrayList<>() : filters; }
    public List<String> getMetrics() { return metrics; }
    public void setMetrics(List<String> metrics) { this.metrics = metrics == null ? new ArrayList<>() : metrics; }
    public List<String> getGroupBy() { return groupBy; }
    public void setGroupBy(List<String> groupBy) { this.groupBy = groupBy == null ? new ArrayList<>() : groupBy; }
    public List<QueryOrder> getOrderBy() { return orderBy; }
    public void setOrderBy(List<QueryOrder> orderBy) { this.orderBy = orderBy == null ? new ArrayList<>() : orderBy; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
