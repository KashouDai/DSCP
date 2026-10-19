package com.dop.digital.service;

import com.dop.digital.domain.DigitalDashboardOverview;
import java.util.Map;

/**
 * 数码业务首页看板 服务层
 *
 * @author dop
 */
public interface IDigitalDashboardService
{
    /**
     * 查询首页看板概览
     *
     * @return 首页看板概览
     */
    public DigitalDashboardOverview selectDashboardOverview();

    public Map<String, Object> queryAnalytics(String metric, Map<String, Object> filters);
}
