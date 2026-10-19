package com.dop.digital.agent.query.domain;

import java.util.List;
import java.util.Map;

public class AgentQueryResult
{
    private Map<String, Object> summary;
    private List<Map<String, Object>> rows;
    private List<Map<String, String>> columns;
    private Map<String, Object> queryMeta;

    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public List<Map<String, String>> getColumns() { return columns; }
    public void setColumns(List<Map<String, String>> columns) { this.columns = columns; }
    public Map<String, Object> getQueryMeta() { return queryMeta; }
    public void setQueryMeta(Map<String, Object> queryMeta) { this.queryMeta = queryMeta; }
}
