package com.dop.digital.service;

import java.util.List;
import com.dop.digital.domain.DigitalSaleOrder;

/**
 * 销售订单 服务层
 *
 * @author dop
 */
public interface IDigitalSaleOrderService
{
    /**
     * 查询销售订单列表
     *
     * @param order 销售订单
     * @return 销售订单集合
     */
    public List<DigitalSaleOrder> selectDigitalSaleOrderList(DigitalSaleOrder order);

    /**
     * 根据订单ID查询销售订单
     *
     * @param orderId 订单ID
     * @return 销售订单
     */
    public DigitalSaleOrder selectDigitalSaleOrderById(Long orderId);

    /**
     * 新增草稿订单
     *
     * @param order 销售订单
     * @param operator 操作人
     * @return 销售订单
     */
    public DigitalSaleOrder createDigitalSaleOrderDraft(DigitalSaleOrder order, String operator);

    /**
     * 修改草稿订单
     *
     * @param order 销售订单
     * @param operator 操作人
     * @return 销售订单
     */
    public DigitalSaleOrder updateDigitalSaleOrderDraft(DigitalSaleOrder order, String operator);

    /**
     * 确认订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 销售订单
     */
    public DigitalSaleOrder confirmDigitalSaleOrder(Long orderId, String operator);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 销售订单
     */
    public DigitalSaleOrder cancelDigitalSaleOrder(Long orderId, String operator);

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 销售订单
     */
    public DigitalSaleOrder completeDigitalSaleOrder(Long orderId, String operator);

    /**
     * 批量删除草稿订单
     *
     * @param orderIds 订单ID数组
     * @return 结果
     */
    public int deleteDigitalSaleOrderByIds(Long[] orderIds);
}
