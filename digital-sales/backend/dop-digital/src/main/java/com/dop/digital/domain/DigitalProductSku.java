package com.dop.digital.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.dop.common.annotation.Excel;
import com.dop.common.core.domain.BaseEntity;

/**
 * 商品规格对象 biz_product_sku
 *
 * @author dop
 */
public class DigitalProductSku extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规格ID */
    private Long skuId;

    /** 商品ID */
    private Long productId;

    /** 规格编码 */
    @Excel(name = "规格编码")
    private String skuCode;

    /** 规格名称 */
    @Excel(name = "规格名称")
    private String skuName;

    /** 销售价 */
    @Excel(name = "销售价")
    private BigDecimal salePrice;

    /** 成本价 */
    @Excel(name = "成本价")
    private BigDecimal costPrice;

    /** 条码 */
    @Excel(name = "条码")
    private String barcode;

    /** 当前可售库存 */
    @Excel(name = "当前库存")
    private Integer stock;

    /** 库存预警值 */
    @Excel(name = "库存预警值")
    private Integer warnStock;

    /** 状态（0上架 1下架） */
    @Excel(name = "状态", readConverterExp = "0=上架,1=下架")
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String categoryName;

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    @NotNull(message = "商品不能为空")
    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    @NotBlank(message = "规格编码不能为空")
    @Size(max = 64, message = "规格编码长度不能超过64个字符")
    public String getSkuCode()
    {
        return skuCode;
    }

    public void setSkuCode(String skuCode)
    {
        this.skuCode = skuCode;
    }

    @NotBlank(message = "规格名称不能为空")
    @Size(max = 100, message = "规格名称长度不能超过100个字符")
    public String getSkuName()
    {
        return skuName;
    }

    public void setSkuName(String skuName)
    {
        this.skuName = skuName;
    }

    @NotNull(message = "销售价不能为空")
    @DecimalMin(value = "0.00", message = "销售价不能小于0")
    public BigDecimal getSalePrice()
    {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice)
    {
        this.salePrice = salePrice;
    }

    @DecimalMin(value = "0.00", message = "成本价不能小于0")
    public BigDecimal getCostPrice()
    {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice)
    {
        this.costPrice = costPrice;
    }

    @Size(max = 64, message = "条码长度不能超过64个字符")
    public String getBarcode()
    {
        return barcode;
    }

    public void setBarcode(String barcode)
    {
        this.barcode = barcode;
    }

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    @NotNull(message = "库存预警值不能为空")
    @Min(value = 0, message = "库存预警值不能小于0")
    public Integer getWarnStock()
    {
        return warnStock;
    }

    public void setWarnStock(Integer warnStock)
    {
        this.warnStock = warnStock;
    }

    @NotBlank(message = "状态不能为空")
    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("skuId", getSkuId())
            .append("productId", getProductId())
            .append("skuCode", getSkuCode())
            .append("skuName", getSkuName())
            .append("salePrice", getSalePrice())
            .append("costPrice", getCostPrice())
            .append("barcode", getBarcode())
            .append("stock", getStock())
            .append("warnStock", getWarnStock())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("productName", getProductName())
            .append("categoryName", getCategoryName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
