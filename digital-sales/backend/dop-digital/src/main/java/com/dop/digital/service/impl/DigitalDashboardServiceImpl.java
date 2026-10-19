package com.dop.digital.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;
import com.dop.digital.domain.DigitalDashboardOverview;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.mapper.DigitalDashboardMapper;
import com.dop.digital.service.IDigitalDashboardService;

/**
 * 数码业务首页看板 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalDashboardServiceImpl implements IDigitalDashboardService
{
    @Autowired
    private DigitalDashboardMapper dashboardMapper;

    @Override
    public DigitalDashboardOverview selectDashboardOverview()
    {
        DigitalDashboardOverview overview = new DigitalDashboardOverview();
        Long orderCount = dashboardMapper.selectTodayOrderCount();
        BigDecimal salesAmount = dashboardMapper.selectTodaySalesAmount();
        BigDecimal refundAmount = dashboardMapper.selectTodayRefundAmount();
        Long pendingTicketCount = dashboardMapper.selectPendingTicketCount();
        List<DigitalProductSku> lowStockList = dashboardMapper.selectLowStockList();

        overview.setTodayOrderCount(orderCount == null ? 0L : orderCount);
        overview.setTodaySalesAmount(salesAmount == null ? BigDecimal.ZERO : salesAmount);
        overview.setTodayRefundAmount(refundAmount == null ? BigDecimal.ZERO : refundAmount);
        overview.setPendingTicketCount(pendingTicketCount == null ? 0L : pendingTicketCount);
        overview.setLowStockList(lowStockList == null ? Collections.emptyList() : lowStockList);
        return overview;
    }

    @Override
    public Map<String, Object> queryAnalytics(String metric, Map<String, Object> filters)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metric", metric);
        switch (metric)
        {
            case "customer_count":
                result.put("value", dashboardMapper.selectCustomerCount(filters));
                break;
            case "order_count":
                result.put("value", dashboardMapper.selectOrderCount(filters));
                break;
            case "order_amount_sum":
                result.put("value", dashboardMapper.selectOrderAmount(filters));
                break;
            case "order_amount_avg":
                Long orderCount = dashboardMapper.selectOrderCount(filters);
                BigDecimal orderAmount = dashboardMapper.selectOrderAmount(filters);
                result.put("value", orderCount == null || orderCount == 0 ? BigDecimal.ZERO : orderAmount.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP));
                break;
            case "refund_count":
                result.put("value", dashboardMapper.selectRefundCount(filters));
                break;
            case "refund_amount_sum":
                result.put("value", dashboardMapper.selectRefundAmount(filters));
                break;
            case "net_sales_amount":
                result.put("value", dashboardMapper.selectOrderAmount(filters).subtract(dashboardMapper.selectRefundAmount(filters)));
                break;
            case "low_stock_count":
                result.put("value", dashboardMapper.selectLowStockCount());
                break;
            case "customer_order_summary":
                result.put("rows", dashboardMapper.selectCustomerOrderSummary(filters));
                break;
            case "customer_business_summary":
                result.put("rows", dashboardMapper.selectCustomerBusinessSummary(filters));
                break;
            case "order_status_summary":
                result.put("rows", dashboardMapper.selectOrderStatusSummary(filters));
                break;
            case "monthly_order_amount_summary":
                result.put("rows", dashboardMapper.selectMonthlyOrderAmountSummary(filters));
                break;
            default:
                throw new IllegalArgumentException("不支持的统计指标：" + metric);
        }
        return result;
    }
}
