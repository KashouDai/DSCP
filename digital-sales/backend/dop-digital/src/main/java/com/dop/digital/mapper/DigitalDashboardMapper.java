package com.dop.digital.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.dop.digital.domain.DigitalProductSku;

/**
 * 数码业务首页看板 数据层
 *
 * @author dop
 */
public interface DigitalDashboardMapper
{
    /**
     * 查询今日成交单数
     *
     * @return 今日成交单数
     */
    public Long selectTodayOrderCount();

    /**
     * 查询今日成交金额
     *
     * @return 今日成交金额
     */
    public BigDecimal selectTodaySalesAmount();

    /**
     * 查询今日退款金额
     *
     * @return 今日退款金额
     */
    public BigDecimal selectTodayRefundAmount();

    /**
     * 查询待处理工单数
     *
     * @return 待处理工单数
     */
    public Long selectPendingTicketCount();

    /**
     * 查询低库存规格
     *
     * @return 低库存规格集合
     */
    public List<DigitalProductSku> selectLowStockList();

    public Long selectCustomerCount(Map<String, Object> filters);

    public Long selectOrderCount(Map<String, Object> filters);

    public BigDecimal selectOrderAmount(Map<String, Object> filters);

    public Long selectRefundCount(Map<String, Object> filters);

    public BigDecimal selectRefundAmount(Map<String, Object> filters);

    public Long selectLowStockCount();

    public List<Map<String, Object>> selectCustomerOrderSummary(Map<String, Object> filters);

    public List<Map<String, Object>> selectCustomerBusinessSummary(Map<String, Object> filters);

    public List<Map<String, Object>> selectOrderStatusSummary(Map<String, Object> filters);

    public List<Map<String, Object>> selectMonthlyOrderAmountSummary(Map<String, Object> filters);
}
