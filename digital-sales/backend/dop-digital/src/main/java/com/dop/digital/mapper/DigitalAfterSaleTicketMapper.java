package com.dop.digital.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.dop.digital.domain.DigitalAfterSaleTicket;

/**
 * 售后工单 数据层
 *
 * @author dop
 */
public interface DigitalAfterSaleTicketMapper
{
    /**
     * 查询售后工单列表
     *
     * @param ticket 售后工单
     * @return 售后工单集合
     */
    public List<DigitalAfterSaleTicket> selectDigitalAfterSaleTicketList(DigitalAfterSaleTicket ticket);

    /**
     * 根据工单ID查询售后工单
     *
     * @param ticketId 工单ID
     * @return 售后工单
     */
    public DigitalAfterSaleTicket selectDigitalAfterSaleTicketById(Long ticketId);

    /**
     * 根据工单ID锁定售后工单
     *
     * @param ticketId 工单ID
     * @return 售后工单
     */
    public DigitalAfterSaleTicket selectDigitalAfterSaleTicketByIdForUpdate(Long ticketId);

    /**
     * 新增售后工单
     *
     * @param ticket 售后工单
     * @return 结果
     */
    public int insertDigitalAfterSaleTicket(DigitalAfterSaleTicket ticket);

    /**
     * 分配处理人
     *
     * @param ticketId 工单ID
     * @param handlerId 处理人ID
     * @param operator 操作人
     * @return 结果
     */
    public int assignDigitalAfterSaleTicket(@Param("ticketId") Long ticketId,
            @Param("handlerId") Long handlerId, @Param("operator") String operator);

    /**
     * 开始处理或记录处理进展
     *
     * @param ticket 售后工单
     * @return 结果
     */
    public int handleDigitalAfterSaleTicket(DigitalAfterSaleTicket ticket);

    /**
     * 完成维修工单
     *
     * @param ticketId 工单ID
     * @param handleResult 处理结果
     * @param operator 操作人
     * @return 结果
     */
    public int completeDigitalAfterSaleTicket(@Param("ticketId") Long ticketId,
            @Param("handleResult") String handleResult, @Param("operator") String operator);

    /**
     * 完成退货检查
     *
     * @param ticketId 工单ID
     * @param receivedQuantity 实收数量
     * @param handlingResult 商品处理结论
     * @param handleResult 处理结果
     * @param operator 操作人
     * @return 结果
     */
    public int completeReturnDigitalAfterSaleTicket(@Param("ticketId") Long ticketId,
            @Param("receivedQuantity") Integer receivedQuantity,
            @Param("handlingResult") String handlingResult,
            @Param("handleResult") String handleResult, @Param("operator") String operator);

    /**
     * 关闭售后工单
     *
     * @param ticketId 工单ID
     * @param closeReason 关闭原因
     * @param operator 操作人
     * @return 结果
     */
    public int closeDigitalAfterSaleTicket(@Param("ticketId") Long ticketId,
            @Param("closeReason") String closeReason, @Param("operator") String operator);

    /**
     * 查询订单明细上未结束的工单数量
     *
     * @param orderItemId 订单明细ID
     * @return 工单数量
     */
    public int countActiveTickets(@Param("orderItemId") Long orderItemId);

    /**
     * 查询订单明细上未结束的退货工单数量
     *
     * @param orderItemId 订单明细ID
     * @return 工单数量
     */
    public int countActiveReturnTickets(@Param("orderItemId") Long orderItemId);

    /**
     * 查询订单明细上指定类型的未结束工单申请数量
     *
     * @param orderItemId 订单明细ID
     * @param ticketType 工单类型
     * @return 申请数量
     */
    public int sumActiveTicketQuantity(@Param("orderItemId") Long orderItemId,
            @Param("ticketType") String ticketType);
}
