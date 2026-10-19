package com.dop.digital.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.dop.common.annotation.Excel;
import com.dop.common.core.domain.BaseEntity;

/**
 * 库存流水对象 biz_stock_record
 *
 * @author dop
 */
public class DigitalStockRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 流水ID */
    private Long recordId;

    /** 规格ID */
    private Long skuId;

    /** 流水类型 */
    @Excel(name = "流水类型")
    private String recordType;

    /** 出入库方向（1入库 -1出库） */
    @Excel(name = "出入库方向")
    private Integer changeDirection;

    /** 变更数量 */
    @Excel(name = "变更数量")
    private Integer quantity;

    /** 变更前库存 */
    @Excel(name = "变更前库存")
    private Integer beforeStock;

    /** 变更后库存 */
    @Excel(name = "变更后库存")
    private Integer afterStock;

    /** 业务类型 */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 手工入库幂等键 */
    private String requestNo;

    /** 规格编码 */
    @Excel(name = "规格编码")
    private String skuCode;

    /** 规格名称 */
    @Excel(name = "规格名称")
    private String skuName;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String productName;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public String getRecordType()
    {
        return recordType;
    }

    public void setRecordType(String recordType)
    {
        this.recordType = recordType;
    }

    public Integer getChangeDirection()
    {
        return changeDirection;
    }

    public void setChangeDirection(Integer changeDirection)
    {
        this.changeDirection = changeDirection;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public Integer getBeforeStock()
    {
        return beforeStock;
    }

    public void setBeforeStock(Integer beforeStock)
    {
        this.beforeStock = beforeStock;
    }

    public Integer getAfterStock()
    {
        return afterStock;
    }

    public void setAfterStock(Integer afterStock)
    {
        this.afterStock = afterStock;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public String getRequestNo()
    {
        return requestNo;
    }

    public void setRequestNo(String requestNo)
    {
        this.requestNo = requestNo;
    }

    public String getSkuCode()
    {
        return skuCode;
    }

    public void setSkuCode(String skuCode)
    {
        this.skuCode = skuCode;
    }

    public String getSkuName()
    {
        return skuName;
    }

    public void setSkuName(String skuName)
    {
        this.skuName = skuName;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("skuId", getSkuId())
            .append("recordType", getRecordType())
            .append("changeDirection", getChangeDirection())
            .append("quantity", getQuantity())
            .append("beforeStock", getBeforeStock())
            .append("afterStock", getAfterStock())
            .append("bizType", getBizType())
            .append("bizId", getBizId())
            .append("requestNo", getRequestNo())
            .append("skuCode", getSkuCode())
            .append("skuName", getSkuName())
            .append("productName", getProductName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}
