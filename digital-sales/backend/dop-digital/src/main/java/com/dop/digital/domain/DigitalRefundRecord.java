package com.dop.digital.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.dop.common.core.domain.BaseEntity;

/**
 * 退款记录对象 biz_refund_record
 *
 * @author dop
 */
public class DigitalRefundRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 退款ID */
    private Long refundId;

    /** 售后工单ID */
    private Long ticketId;

    /** 工单编号 */
    private String ticketNo;

    /** 订单明细ID */
    private Long orderItemId;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 客户名称快照 */
    private String customerName;

    /** 规格ID */
    private Long skuId;

    /** 规格编码 */
    private String skuCode;

    /** 商品名称快照 */
    private String productName;

    /** 规格名称快照 */
    private String skuName;

    /** 成交单价快照 */
    private BigDecimal salePrice;

    /** 购买数量 */
    private Integer purchaseQuantity;

    /** 实收数量 */
    private Integer receivedQuantity;

    /** 商品处理结论 */
    private String handlingResult;

    /** 退款金额 */
    @DecimalMin(value = "0.00", message = "退款金额不能小于0")
    @Digits(integer = 10, fraction = 2, message = "退款金额最多保留两位小数")
    private BigDecimal refundAmount;

    /** 订单明细已退金额 */
    private BigDecimal refundedAmount;

    /** 退款状态 */
    private String status;

    /** 处理人ID */
    private Long handlerId;

    /** 处理人名称 */
    private String handlerName;

    /** 处理时间 */
    private Date handleTime;

    public Long getRefundId()
    {
        return refundId;
    }

    public void setRefundId(Long refundId)
    {
        this.refundId = refundId;
    }

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    public String getTicketNo()
    {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo)
    {
        this.ticketNo = ticketNo;
    }

    public Long getOrderItemId()
    {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId)
    {
        this.orderItemId = orderItemId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public String getSkuCode()
    {
        return skuCode;
    }

    public void setSkuCode(String skuCode)
    {
        this.skuCode = skuCode;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getSkuName()
    {
        return skuName;
    }

    public void setSkuName(String skuName)
    {
        this.skuName = skuName;
    }

    public BigDecimal getSalePrice()
    {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    public Integer getPurchaseQuantity()
    {
        return purchaseQuantity;
    }

    public void setPurchaseQuantity(Integer purchaseQuantity)
    {
        this.purchaseQuantity = purchaseQuantity;
    }

    public Integer getReceivedQuantity()
    {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity)
    {
        this.receivedQuantity = receivedQuantity;
    }

    public String getHandlingResult()
    {
        return handlingResult;
    }

    public void setHandlingResult(String handlingResult)
    {
        this.handlingResult = handlingResult;
    }

    public BigDecimal getRefundAmount()
    {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount)
    {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getRefundedAmount()
    {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount)
    {
        this.refundedAmount = refundedAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getHandlerId()
    {
        return handlerId;
    }

    public void setHandlerId(Long handlerId)
    {
        this.handlerId = handlerId;
    }

    public String getHandlerName()
    {
        return handlerName;
    }

    public void setHandlerName(String handlerName)
    {
        this.handlerName = handlerName;
    }

    public Date getHandleTime()
    {
        return handleTime;
    }

    public void setHandleTime(Date handleTime)
    {
        this.handleTime = handleTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("refundId", getRefundId())
            .append("ticketId", getTicketId())
            .append("ticketNo", getTicketNo())
            .append("orderItemId", getOrderItemId())
            .append("orderId", getOrderId())
            .append("orderNo", getOrderNo())
            .append("customerName", getCustomerName())
            .append("skuId", getSkuId())
            .append("skuCode", getSkuCode())
            .append("productName", getProductName())
            .append("skuName", getSkuName())
            .append("salePrice", getSalePrice())
            .append("purchaseQuantity", getPurchaseQuantity())
            .append("receivedQuantity", getReceivedQuantity())
            .append("handlingResult", getHandlingResult())
            .append("refundAmount", getRefundAmount())
            .append("refundedAmount", getRefundedAmount())
            .append("status", getStatus())
            .append("handlerId", getHandlerId())
            .append("handleTime", getHandleTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
