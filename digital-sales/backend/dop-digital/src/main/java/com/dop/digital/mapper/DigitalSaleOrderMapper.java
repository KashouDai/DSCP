package com.dop.digital.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.dop.digital.domain.DigitalSaleOrder;

/**
 * 销售订单 数据层
 *
 * @author dop
 */
public interface DigitalSaleOrderMapper
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
     * 根据订单ID查询销售订单并锁定
     *
     * @param orderId 订单ID
     * @return 销售订单
     */
    public DigitalSaleOrder selectDigitalSaleOrderByIdForUpdate(Long orderId);

    /**
     * 根据客户端请求号查询销售订单
     *
     * @param requestNo 客户端请求号
     * @return 销售订单
     */
    public DigitalSaleOrder selectDigitalSaleOrderByRequestNo(String requestNo);

    public int countOrderByCustomerId(@Param("customerId") Long customerId);

    /**
     * 根据订单ID集合查询销售订单
     *
     * @param orderIds 订单ID数组
     * @return 销售订单集合
     */
    public List<DigitalSaleOrder> selectDigitalSaleOrderByIds(@Param("orderIds") Long[] orderIds);

    /**
     * 新增销售订单
     *
     * @param order 销售订单
     * @return 结果
     */
    public int insertDigitalSaleOrder(DigitalSaleOrder order);

    /**
     * 修改草稿销售订单
     *
     * @param order 销售订单
     * @return 结果
     */
    public int updateDigitalSaleOrderDraft(DigitalSaleOrder order);

    /**
     * 确认订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 结果
     */
    public int confirmDigitalSaleOrder(@Param("orderId") Long orderId, @Param("operator") String operator);

    /**
     * 取消草稿订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 结果
     */
    public int cancelDigitalSaleOrderDraft(@Param("orderId") Long orderId, @Param("operator") String operator);

    /**
     * 取消已确认订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 结果
     */
    public int cancelDigitalSaleOrderConfirmed(@Param("orderId") Long orderId, @Param("operator") String operator);

    /**
     * 完成订单
     *
     * @param orderId 订单ID
     * @param operator 操作人
     * @return 结果
     */
    public int completeDigitalSaleOrder(@Param("orderId") Long orderId, @Param("operator") String operator);

    /**
     * 批量删除草稿订单
     *
     * @param orderIds 订单ID数组
     * @return 结果
     */
    public int deleteDigitalSaleOrderByIds(@Param("orderIds") Long[] orderIds);
}
