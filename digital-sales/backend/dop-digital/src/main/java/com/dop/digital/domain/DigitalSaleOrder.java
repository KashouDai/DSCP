package com.dop.digital.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.dop.common.annotation.Excel;
import com.dop.common.core.domain.BaseEntity;

/**
 * 销售订单对象 biz_sale_order
 *
 * @author dop
 */
public class DigitalSaleOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单编号 */
    @Excel(name = "订单编号")
    private String orderNo;

    /** 客户端订单创建请求号 */
    private String requestNo;

    /** 订单创建请求摘要，仅用于幂等校验 */
    private String requestHash;

    /** 客户ID */
    private Long customerId;

    /** 客户名称快照 */
    @Excel(name = "客户名称")
    private String customerName;

    /** 客户联系方式快照 */
    private String customerPhone;

    /** 订单总金额 */
    @Excel(name = "订单金额")
    private BigDecimal totalAmount;

    /** 订单状态 */
    @Excel(name = "订单状态", readConverterExp = "DRAFT=草稿,CONFIRMED=已确认,COMPLETED=已完成,CANCELLED=已取消")
    private String status;

    /** 确认人 */
    private String confirmBy;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    /** 完成人 */
    private String completeBy;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;

    /** 取消人 */
    private String cancelBy;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 订单明细 */
    private List<DigitalSaleOrderItem> items;

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

    public String getRequestNo()
    {
        return requestNo;
    }

    public void setRequestNo(String requestNo)
    {
        this.requestNo = requestNo;
    }

    @JsonIgnore
    public String getRequestHash()
    {
        return requestHash;
    }

    public void setRequestHash(String requestHash)
    {
        this.requestHash = requestHash;
    }

    @NotNull(message = "客户不能为空")
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

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getConfirmBy()
    {
        return confirmBy;
    }

    public void setConfirmBy(String confirmBy)
    {
        this.confirmBy = confirmBy;
    }

    public Date getConfirmTime()
    {
        return confirmTime;
    }

    public void setConfirmTime(Date confirmTime)
    {
        this.confirmTime = confirmTime;
    }

    public String getCompleteBy()
    {
        return completeBy;
    }

    public void setCompleteBy(String completeBy)
    {
        this.completeBy = completeBy;
    }

    public Date getCompleteTime()
    {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime)
    {
        this.completeTime = completeTime;
    }

    public String getCancelBy()
    {
        return cancelBy;
    }

    public void setCancelBy(String cancelBy)
    {
        this.cancelBy = cancelBy;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    public List<DigitalSaleOrderItem> getItems()
    {
        return items;
    }

    public void setItems(List<DigitalSaleOrderItem> items)
    {
        this.items = items;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("orderNo", getOrderNo())
            .append("requestNo", getRequestNo())
            .append("customerId", getCustomerId())
            .append("customerName", getCustomerName())
            .append("customerPhone", getCustomerPhone())
            .append("totalAmount", getTotalAmount())
            .append("status", getStatus())
            .append("confirmBy", getConfirmBy())
            .append("confirmTime", getConfirmTime())
            .append("completeBy", getCompleteBy())
            .append("completeTime", getCompleteTime())
            .append("cancelBy", getCancelBy())
            .append("cancelTime", getCancelTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
