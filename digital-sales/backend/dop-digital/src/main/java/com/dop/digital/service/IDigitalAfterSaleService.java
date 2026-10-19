package com.dop.digital.service;

import java.util.List;
import com.dop.common.core.domain.entity.SysUser;
import com.dop.digital.domain.DigitalAfterSaleTicket;

/**
 * 售后工单 服务层
 *
 * @author dop
 */
public interface IDigitalAfterSaleService
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
     * 从已完成订单明细创建售后工单
     *
     * @param ticket 售后工单
     * @param operator 操作人
     * @return 售后工单
     */
    public DigitalAfterSaleTicket createDigitalAfterSaleTicket(DigitalAfterSaleTicket ticket, String operator);

    /**
     * 分配处理人
     *
     * @param ticketId 工单ID
     * @param handlerId 处理人ID
     * @param operator 操作人
     * @return 售后工单
     */
    public DigitalAfterSaleTicket assignDigitalAfterSaleTicket(Long ticketId, Long handlerId, String operator);

    /**
     * 记录处理进展
     *
     * @param ticket 售后工单
     * @param operator 操作人
     * @return 售后工单
     */
    public DigitalAfterSaleTicket handleDigitalAfterSaleTicket(DigitalAfterSaleTicket ticket, String operator);

    /**
     * 完成维修工单
     *
     * @param ticketId 工单ID
     * @param handleResult 处理结果
     * @param operator 操作人
     * @return 售后工单
     */
    public DigitalAfterSaleTicket completeDigitalAfterSaleTicket(Long ticketId, String handleResult, String operator);

    /**
     * 退货检查并登记实收数量和处理结论
     *
     * @param ticketId 工单ID
     * @param ticket 退货检查请求
     * @param operator 操作人
     * @return 售后工单
     */
    public DigitalAfterSaleTicket checkReturnDigitalAfterSaleTicket(Long ticketId,
            DigitalAfterSaleTicket ticket, String operator);

    /**
     * 关闭售后工单
     *
     * @param ticketId 工单ID
     * @param closeReason 关闭原因
     * @param operator 操作人
     * @return 售后工单
     */
    public DigitalAfterSaleTicket closeDigitalAfterSaleTicket(Long ticketId, String closeReason, String operator);

    /**
     * 查询可选处理人
     *
     * @return 正常状态用户集合
     */
    public List<SysUser> selectHandlerList();
}
