package com.dop.digital.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.dop.digital.domain.DigitalSaleOrderItem;

/**
 * 销售订单明细 数据层
 *
 * @author dop
 */
public interface DigitalSaleOrderItemMapper
{
    /**
     * 查询订单明细
     *
     * @param orderId 订单ID
     * @return 订单明细集合
     */
    public List<DigitalSaleOrderItem> selectDigitalSaleOrderItemList(@Param("orderId") Long orderId);

    public int countOrderItemBySkuId(@Param("skuId") Long skuId);

    /**
     * 根据订单明细ID锁定订单明细
     *
     * @param itemId 明细ID
     * @return 订单明细
     */
    public DigitalSaleOrderItem selectDigitalSaleOrderItemByIdForUpdate(@Param("itemId") Long itemId);

    /**
     * 批量新增订单明细
     *
     * @param items 订单明细集合
     * @return 结果
     */
    public int batchInsertDigitalSaleOrderItem(@Param("items") List<DigitalSaleOrderItem> items);

    /**
     * 根据订单ID删除订单明细
     *
     * @param orderId 订单ID
     * @return 结果
     */
    public int deleteDigitalSaleOrderItemByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单ID集合删除订单明细
     *
     * @param orderIds 订单ID数组
     * @return 结果
     */
    public int deleteDigitalSaleOrderItemByOrderIds(@Param("orderIds") Long[] orderIds);

    /**
     * 累计已退数量
     *
     * @param itemId 明细ID
     * @param quantity 实收数量
     * @param operator 操作人
     * @return 结果
     */
    public int increaseRefundedQuantity(@Param("itemId") Long itemId,
            @Param("quantity") Integer quantity, @Param("operator") String operator);

    /**
     * 累计已退金额
     *
     * @param itemId 明细ID
     * @param amount 退款金额
     * @param operator 操作人
     * @return 结果
     */
    public int increaseRefundedAmount(@Param("itemId") Long itemId,
            @Param("amount") BigDecimal amount, @Param("operator") String operator);
}
