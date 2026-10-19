package com.dop.digital.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.dop.common.core.domain.BaseEntity;

/**
 * 售后工单对象 biz_after_sale_ticket
 *
 * @author dop
 */
public class DigitalAfterSaleTicket extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long ticketId;

    /** 工单编号 */
    private String ticketNo;

    /** 工单类型（RETURN退货 REPAIR维修） */
    private String ticketType;

    /** 订单明细ID */
    private Long orderItemId;

    /** 来源维修工单ID */
    private Long sourceTicketId;

    /** 售后时效豁免原因 */
    private String validityExemptReason;

    /** 申请数量 */
    private Integer applyQuantity;

    /** 实收数量 */
    private Integer receivedQuantity;

    /** 商品处理结论（GOOD完好 DAMAGED损坏） */
    private String handlingResult;

    /** 工单状态 */
    private String status;

    /** 处理人ID */
    private Long handlerId;

    /** 故障描述 */
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 500, message = "问题描述不能超过500个字符")
    private String faultDesc;

    /** 处理结果 */
    private String handleResult;

    /** 图片附件 */
    private String imageUrls;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    private String orderNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称快照 */
    private String customerName;

    /** 客户联系方式快照 */
    private String customerPhone;

    /** 订单完成时间 */
    private Date orderCompleteTime;

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

    /** 已退数量 */
    private Integer refundedQuantity;

    /** 处理人名称 */
    private String handlerName;

    /** 处理日志 */
    private List<DigitalAfterSaleLog> logs;

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

    @NotBlank(message = "工单类型不能为空")
    public String getTicketType()
    {
        return ticketType;
    }

    public void setTicketType(String ticketType)
    {
        this.ticketType = ticketType;
    }

    @NotNull(message = "订单明细不能为空")
    public Long getOrderItemId()
    {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId)
    {
        this.orderItemId = orderItemId;
    }

    public Long getSourceTicketId()
    {
        return sourceTicketId;
    }

    public void setSourceTicketId(Long sourceTicketId)
    {
        this.sourceTicketId = sourceTicketId;
    }

    @Size(max = 500, message = "时效豁免原因长度不能超过500个字符")
    public String getValidityExemptReason()
    {
        return validityExemptReason;
    }

    public void setValidityExemptReason(String validityExemptReason)
    {
        this.validityExemptReason = validityExemptReason;
    }

    @NotNull(message = "申请数量不能为空")
    @Min(value = 1, message = "申请数量必须大于0")
    public Integer getApplyQuantity()
    {
        return applyQuantity;
    }

    public void setApplyQuantity(Integer applyQuantity)
    {
        this.applyQuantity = applyQuantity;
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

    @Size(max = 1000, message = "故障描述长度不能超过1000个字符")
    public String getFaultDesc()
    {
        return faultDesc;
    }

    public void setFaultDesc(String faultDesc)
    {
        this.faultDesc = faultDesc;
    }

    @Size(max = 1000, message = "处理结果长度不能超过1000个字符")
    public String getHandleResult()
    {
        return handleResult;
    }

    public void setHandleResult(String handleResult)
    {
        this.handleResult = handleResult;
    }

    @Size(max = 2000, message = "图片附件长度不能超过2000个字符")
    public String getImageUrls()
    {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls)
    {
        this.imageUrls = imageUrls;
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

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getCustomerName()
    {
        return customerName;
    }

    public void setCustomerName(String customerName)
    {
        this.customerName = customerName;
    }

    public String getCustomerPhone()
    {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone)
    {
        this.customerPhone = customerPhone;
    }

    public Date getOrderCompleteTime()
    {
        return orderCompleteTime;
    }

    public void setOrderCompleteTime(Date orderCompleteTime)
    {
        this.orderCompleteTime = orderCompleteTime;
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

    public Integer getRefundedQuantity()
    {
        return refundedQuantity;
    }

    public void setRefundedQuantity(Integer refundedQuantity)
    {
        this.refundedQuantity = refundedQuantity;
    }

    public String getHandlerName()
    {
        return handlerName;
    }

    public void setHandlerName(String handlerName)
    {
        this.handlerName = handlerName;
    }

    public List<DigitalAfterSaleLog> getLogs()
    {
        return logs;
    }

    public void setLogs(List<DigitalAfterSaleLog> logs)
    {
        this.logs = logs;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("ticketId", getTicketId())
            .append("ticketNo", getTicketNo())
            .append("ticketType", getTicketType())
            .append("orderItemId", getOrderItemId())
            .append("sourceTicketId", getSourceTicketId())
            .append("validityExemptReason", getValidityExemptReason())
            .append("applyQuantity", getApplyQuantity())
            .append("receivedQuantity", getReceivedQuantity())
            .append("handlingResult", getHandlingResult())
            .append("status", getStatus())
            .append("handlerId", getHandlerId())
            .append("faultDesc", getFaultDesc())
            .append("handleResult", getHandleResult())
            .append("imageUrls", getImageUrls())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
