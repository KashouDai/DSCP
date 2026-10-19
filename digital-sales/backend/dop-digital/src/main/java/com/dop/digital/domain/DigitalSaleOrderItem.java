package com.dop.digital.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.dop.common.annotation.Excel;
import com.dop.common.core.domain.BaseEntity;

/**
 * 销售订单明细对象 biz_sale_order_item
 *
 * @author dop
 */
public class DigitalSaleOrderItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    private Long itemId;

    /** 订单ID */
    private Long orderId;

    /** 规格ID */
    private Long skuId;

    /** 商品名称快照 */
    @Excel(name = "商品名称")
    private String productName;

    /** 规格名称快照 */
    @Excel(name = "规格名称")
    private String skuName;

    /** 成交单价 */
    @Excel(name = "成交单价")
    private BigDecimal salePrice;

    /** 购买数量 */
    @Excel(name = "购买数量")
    private Integer quantity;

    /** 已退数量 */
    private Integer refundedQuantity;

    /** 已退金额 */
    private BigDecimal refundedAmount;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    @NotNull(message = "商品规格不能为空")
    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
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

    @NotNull(message = "成交单价不能为空")
    @DecimalMin(value = "0.00", message = "成交单价不能小于0")
    public BigDecimal getSalePrice()
    {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public Integer getRefundedQuantity()
    {
        return refundedQuantity;
    }

    public void setRefundedQuantity(Integer refundedQuantity)
    {
        this.refundedQuantity = refundedQuantity;
    }

    public BigDecimal getRefundedAmount()
    {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount)
    {
        this.refundedAmount = refundedAmount;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("orderId", getOrderId())
            .append("skuId", getSkuId())
            .append("productName", getProductName())
            .append("skuName", getSkuName())
            .append("salePrice", getSalePrice())
            .append("quantity", getQuantity())
            .append("refundedQuantity", getRefundedQuantity())
            .append("refundedAmount", getRefundedAmount())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
