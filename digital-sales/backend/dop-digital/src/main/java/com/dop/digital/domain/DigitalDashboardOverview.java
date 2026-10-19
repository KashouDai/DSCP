package com.dop.digital.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数码业务首页看板概览
 *
 * @author dop
 */
public class DigitalDashboardOverview
{
    /** 今日成交单数 */
    private Long todayOrderCount;

    /** 今日成交金额 */
    private BigDecimal todaySalesAmount;

    /** 今日退款金额 */
    private BigDecimal todayRefundAmount;

    /** 待处理工单数 */
    private Long pendingTicketCount;

    /** 低库存规格 */
    private List<DigitalProductSku> lowStockList;

    public Long getTodayOrderCount()
    {
        return todayOrderCount;
    }

    public void setTodayOrderCount(Long todayOrderCount)
    {
        this.todayOrderCount = todayOrderCount;
    }

    public BigDecimal getTodaySalesAmount()
    {
        return todaySalesAmount;
    }

    public void setTodaySalesAmount(BigDecimal todaySalesAmount)
    {
        this.todaySalesAmount = todaySalesAmount;
    }

    public BigDecimal getTodayRefundAmount()
    {
        return todayRefundAmount;
    }

    public void setTodayRefundAmount(BigDecimal todayRefundAmount)
    {
        this.todayRefundAmount = todayRefundAmount;
    }

    public Long getPendingTicketCount()
    {
        return pendingTicketCount;
    }

    public void setPendingTicketCount(Long pendingTicketCount)
    {
        this.pendingTicketCount = pendingTicketCount;
    }

    public List<DigitalProductSku> getLowStockList()
    {
        return lowStockList;
    }

    public void setLowStockList(List<DigitalProductSku> lowStockList)
    {
        this.lowStockList = lowStockList;
    }
}
