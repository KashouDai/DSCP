package com.dop.digital.domain;

import java.util.Date;

/** 库存预警对象 biz_stock_alert */
public class DigitalStockAlert
{
    private Long alertId;
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer stock;
    private Integer warnStock;
    private String status;
    private Date triggeredTime;
    private Date resolvedTime;

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getSkuName() { return skuName; }
    public void setSkuName(String skuName) { this.skuName = skuName; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getWarnStock() { return warnStock; }
    public void setWarnStock(Integer warnStock) { this.warnStock = warnStock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTriggeredTime() { return triggeredTime; }
    public void setTriggeredTime(Date triggeredTime) { this.triggeredTime = triggeredTime; }
    public Date getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(Date resolvedTime) { this.resolvedTime = resolvedTime; }
}
