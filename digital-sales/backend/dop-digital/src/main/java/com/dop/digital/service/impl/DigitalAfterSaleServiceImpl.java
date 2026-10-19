package com.dop.digital.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dop.common.core.domain.entity.SysUser;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalAfterSaleLog;
import com.dop.digital.domain.DigitalAfterSaleTicket;
import com.dop.digital.domain.DigitalRefundRecord;
import com.dop.digital.domain.DigitalSaleOrder;
import com.dop.digital.domain.DigitalSaleOrderItem;
import com.dop.digital.mapper.DigitalAfterSaleLogMapper;
import com.dop.digital.mapper.DigitalAfterSaleTicketMapper;
import com.dop.digital.mapper.DigitalRefundRecordMapper;
import com.dop.digital.mapper.DigitalSaleOrderItemMapper;
import com.dop.digital.mapper.DigitalSaleOrderMapper;
import com.dop.digital.service.IDigitalAfterSaleService;
import com.dop.digital.service.IDigitalStockService;
import com.dop.system.service.ISysConfigService;
import com.dop.system.service.ISysUserService;

/**
 * 售后工单 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalAfterSaleServiceImpl implements IDigitalAfterSaleService
{
    private static final String TYPE_RETURN = "RETURN";

    private static final String TYPE_REPAIR = "REPAIR";

    private static final String STATUS_PENDING = "PENDING";

    private static final String STATUS_PROCESSING = "PROCESSING";

    private static final String STATUS_COMPLETED = "COMPLETED";

    private static final String STATUS_CLOSED = "CLOSED";

    private static final String HANDLING_RESULT_GOOD = "GOOD";

    private static final String HANDLING_RESULT_DAMAGED = "DAMAGED";

    private static final String REFUND_STATUS_PENDING = "PENDING";

    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";

    private static final String USER_STATUS_NORMAL = "0";

    private static final String AFTER_SALE_VALID_DAYS_KEY = "digital.afterSale.validDays";

    private static final int DEFAULT_AFTER_SALE_VALID_DAYS = 30;

    private static final int MAX_TICKET_NO_RETRY = 3;

    @Autowired
    private DigitalAfterSaleTicketMapper ticketMapper;

    @Autowired
    private DigitalAfterSaleLogMapper logMapper;

    @Autowired
    private DigitalSaleOrderMapper orderMapper;

    @Autowired
    private DigitalSaleOrderItemMapper orderItemMapper;

    @Autowired
    private DigitalRefundRecordMapper refundMapper;

    @Autowired
    private IDigitalStockService stockService;

    @Autowired
    private DigitalAfterSaleNoGenerator ticketNoGenerator;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysUserService userService;

    @Override
    public List<DigitalAfterSaleTicket> selectDigitalAfterSaleTicketList(DigitalAfterSaleTicket ticket)
    {
        return ticketMapper.selectDigitalAfterSaleTicketList(ticket);
    }

    @Override
    public DigitalAfterSaleTicket selectDigitalAfterSaleTicketById(Long ticketId)
    {
        return loadTicket(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalAfterSaleTicket createDigitalAfterSaleTicket(DigitalAfterSaleTicket ticket, String operator)
    {
        validateCreateRequest(ticket);
        DigitalSaleOrderItem orderItem =
                orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(ticket.getOrderItemId());
        if (StringUtils.isNull(orderItem))
        {
            throw new ServiceException("订单明细不存在");
        }

        DigitalSaleOrder order = orderMapper.selectDigitalSaleOrderById(orderItem.getOrderId());
        if (StringUtils.isNull(order))
        {
            throw new ServiceException("订单不存在");
        }
        if (!ORDER_STATUS_COMPLETED.equals(order.getStatus()))
        {
            throw new ServiceException("只有已完成订单可以申请售后");
        }

        DigitalAfterSaleTicket sourceTicket = validateSourceTicket(ticket);
        validateValidity(ticket, order, sourceTicket);
        validateTicketQuantity(ticket, orderItem);

        ticket.setStatus(STATUS_PENDING);
        ticket.setCreateBy(operator);
        for (int attempt = 0; attempt < MAX_TICKET_NO_RETRY; attempt++)
        {
            ticket.setTicketId(null);
            ticket.setTicketNo(ticketNoGenerator.nextTicketNo());
            try
            {
                if (ticketMapper.insertDigitalAfterSaleTicket(ticket) != 1)
                {
                    throw new ServiceException("创建售后工单失败");
                }
                writeLog(ticket.getTicketId(), "CREATE",
                        buildCreateLog(ticket, orderItem), ticket.getImageUrls(), operator);
                return loadTicket(ticket.getTicketId());
            }
            catch (DuplicateKeyException ex)
            {
                if (attempt == MAX_TICKET_NO_RETRY - 1)
                {
                    throw new ServiceException("生成售后工单编号失败，请重试");
                }
            }
        }
        throw new ServiceException("创建售后工单失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalAfterSaleTicket assignDigitalAfterSaleTicket(Long ticketId, Long handlerId, String operator)
    {
        DigitalAfterSaleTicket ticket = requireTicketForUpdate(ticketId);
        if (StringUtils.isNotNull(handlerId) && handlerId.equals(ticket.getHandlerId()))
        {
            return loadTicket(ticketId);
        }
        requireActiveStatus(ticket, "当前工单不能分配处理人");
        SysUser handler = validateHandler(handlerId);

        if (ticketMapper.assignDigitalAfterSaleTicket(ticketId, handlerId, operator) != 1)
        {
            throw new ServiceException("工单状态已变化，请刷新后重试");
        }
        writeLog(ticketId, "ASSIGN", "分配处理人：" + getDisplayName(handler), null, operator);
        return loadTicket(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalAfterSaleTicket handleDigitalAfterSaleTicket(DigitalAfterSaleTicket request, String operator)
    {
        if (StringUtils.isNull(request) || StringUtils.isNull(request.getTicketId()))
        {
            throw new ServiceException("工单不能为空");
        }
        DigitalAfterSaleTicket ticket = requireTicketForUpdate(request.getTicketId());
        requireActiveStatus(ticket, "当前工单不能记录处理进展");
        if (StringUtils.isEmpty(request.getHandleResult()))
        {
            throw new ServiceException("处理结果不能为空");
        }

        request.setStatus(STATUS_PROCESSING);
        request.setUpdateBy(operator);
        if (ticketMapper.handleDigitalAfterSaleTicket(request) != 1)
        {
            throw new ServiceException("工单状态已变化，请刷新后重试");
        }
        writeLog(ticket.getTicketId(), "HANDLE", request.getHandleResult(),
                request.getImageUrls(), operator);
        return loadTicket(ticket.getTicketId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalAfterSaleTicket completeDigitalAfterSaleTicket(Long ticketId, String handleResult, String operator)
    {
        DigitalAfterSaleTicket ticket = requireTicketForUpdate(ticketId);
        if (!TYPE_REPAIR.equals(ticket.getTicketType()))
        {
            throw new ServiceException("退货工单需在退货检查与退款阶段完成");
        }
        if (STATUS_COMPLETED.equals(ticket.getStatus()))
        {
            return loadTicket(ticketId);
        }
        if (!STATUS_PROCESSING.equals(ticket.getStatus()))
        {
            throw new ServiceException("只有处理中的维修工单可以完成");
        }
        if (StringUtils.isEmpty(handleResult))
        {
            throw new ServiceException("处理结果不能为空");
        }

        if (ticketMapper.completeDigitalAfterSaleTicket(ticketId, handleResult, operator) != 1)
        {
            DigitalAfterSaleTicket current = ticketMapper.selectDigitalAfterSaleTicketById(ticketId);
            if (StringUtils.isNotNull(current) && STATUS_COMPLETED.equals(current.getStatus()))
            {
                return loadTicket(ticketId);
            }
            throw new ServiceException("工单状态已变化，请刷新后重试");
        }
        writeLog(ticketId, "COMPLETE", handleResult, null, operator);
        return loadTicket(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalAfterSaleTicket checkReturnDigitalAfterSaleTicket(Long ticketId,
            DigitalAfterSaleTicket request, String operator)
    {
        DigitalAfterSaleTicket ticket = requireTicketForUpdate(ticketId);
        if (!TYPE_RETURN.equals(ticket.getTicketType()))
        {
            throw new ServiceException("只有退货工单可以进行退货检查");
        }
        if (STATUS_COMPLETED.equals(ticket.getStatus()))
        {
            return loadTicket(ticketId);
        }
        requireActiveStatus(ticket, "当前工单不能进行退货检查");
        validateReturnCheckRequest(ticket, request);

        DigitalSaleOrderItem orderItem =
                orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(ticket.getOrderItemId());
        if (StringUtils.isNull(orderItem))
        {
            throw new ServiceException("订单明细不存在");
        }
        if (orderItemMapper.increaseRefundedQuantity(orderItem.getItemId(),
                request.getReceivedQuantity(), operator) != 1)
        {
            throw new ServiceException("累计退货数量失败");
        }

        DigitalRefundRecord refund = new DigitalRefundRecord();
        refund.setTicketId(ticketId);
        refund.setRefundAmount(java.math.BigDecimal.ZERO);
        refund.setStatus(REFUND_STATUS_PENDING);
        refund.setCreateBy(operator);
        refund.setRemark(request.getRemark());
        if (refundMapper.insertDigitalRefundRecord(refund) != 1)
        {
            throw new ServiceException("创建退款记录失败");
        }

        if (HANDLING_RESULT_GOOD.equals(request.getHandlingResult()))
        {
            stockService.returnInbound(orderItem.getSkuId(), request.getReceivedQuantity(), ticketId,
                    operator, "退货工单：" + ticket.getTicketNo());
        }

        String handleResult = buildReturnCheckResult(request);
        if (ticketMapper.completeReturnDigitalAfterSaleTicket(ticketId,
                request.getReceivedQuantity(), request.getHandlingResult(), handleResult, operator) != 1)
        {
            DigitalAfterSaleTicket current =
                    ticketMapper.selectDigitalAfterSaleTicketById(ticketId);
            if (StringUtils.isNotNull(current) && STATUS_COMPLETED.equals(current.getStatus()))
            {
                return loadTicket(ticketId);
            }
            throw new ServiceException("工单状态已变化，请刷新后重试");
        }
        writeLog(ticketId, "RETURN_CHECK", handleResult, request.getImageUrls(), operator);
        return loadTicket(ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalAfterSaleTicket closeDigitalAfterSaleTicket(Long ticketId, String closeReason, String operator)
    {
        DigitalAfterSaleTicket ticket = requireTicketForUpdate(ticketId);
        if (STATUS_CLOSED.equals(ticket.getStatus()))
        {
            return loadTicket(ticketId);
        }
        requireActiveStatus(ticket, "当前工单不能关闭");
        if (StringUtils.isEmpty(closeReason))
        {
            throw new ServiceException("关闭原因不能为空");
        }

        if (ticketMapper.closeDigitalAfterSaleTicket(ticketId, closeReason, operator) != 1)
        {
            DigitalAfterSaleTicket current = ticketMapper.selectDigitalAfterSaleTicketById(ticketId);
            if (StringUtils.isNotNull(current) && STATUS_CLOSED.equals(current.getStatus()))
            {
                return loadTicket(ticketId);
            }
            throw new ServiceException("工单状态已变化，请刷新后重试");
        }
        writeLog(ticketId, "CLOSE", closeReason, null, operator);
        return loadTicket(ticketId);
    }

    @Override
    public List<SysUser> selectHandlerList()
    {
        SysUser query = new SysUser();
        query.setStatus(USER_STATUS_NORMAL);
        return userService.selectUserList(query);
    }

    private void validateCreateRequest(DigitalAfterSaleTicket ticket)
    {
        if (StringUtils.isNull(ticket) || StringUtils.isNull(ticket.getOrderItemId()))
        {
            throw new ServiceException("订单明细不能为空");
        }
        if (!TYPE_RETURN.equals(ticket.getTicketType()) && !TYPE_REPAIR.equals(ticket.getTicketType()))
        {
            throw new ServiceException("售后类型不正确");
        }
        if (StringUtils.isNull(ticket.getApplyQuantity()) || ticket.getApplyQuantity() <= 0)
        {
            throw new ServiceException("申请数量必须大于0");
        }
    }

    private DigitalAfterSaleTicket validateSourceTicket(DigitalAfterSaleTicket ticket)
    {
        if (StringUtils.isNull(ticket.getSourceTicketId()))
        {
            return null;
        }
        if (!TYPE_RETURN.equals(ticket.getTicketType()))
        {
            throw new ServiceException("只有退货工单可以关联来源维修工单");
        }

        DigitalAfterSaleTicket sourceTicket =
                ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(ticket.getSourceTicketId());
        if (StringUtils.isNull(sourceTicket))
        {
            throw new ServiceException("来源维修工单不存在");
        }
        if (!TYPE_REPAIR.equals(sourceTicket.getTicketType())
                || !STATUS_COMPLETED.equals(sourceTicket.getStatus())
                || !ticket.getOrderItemId().equals(sourceTicket.getOrderItemId()))
        {
            throw new ServiceException("来源维修工单不符合转退货条件");
        }
        if (StringUtils.isEmpty(sourceTicket.getHandleResult())
                || !sourceTicket.getHandleResult().contains("转退货"))
        {
            throw new ServiceException("来源维修工单处理结果未记录转退货");
        }
        return sourceTicket;
    }

    private void validateValidity(DigitalAfterSaleTicket ticket, DigitalSaleOrder order,
            DigitalAfterSaleTicket sourceTicket)
    {
        if (StringUtils.isNull(order.getCompleteTime()))
        {
            throw new ServiceException("订单完成时间缺失，无法校验售后时效");
        }

        Calendar deadline = Calendar.getInstance();
        deadline.setTime(order.getCompleteTime());
        deadline.add(Calendar.DAY_OF_MONTH, getAfterSaleValidDays());
        if (!new Date().after(deadline.getTime()))
        {
            ticket.setValidityExemptReason(null);
            return;
        }
        if (StringUtils.isNull(sourceTicket))
        {
            throw new ServiceException("订单已超过售后时效");
        }
        if (StringUtils.isEmpty(ticket.getValidityExemptReason()))
        {
            ticket.setValidityExemptReason("维修转退货，来源工单：" + sourceTicket.getTicketNo());
        }
    }

    private int getAfterSaleValidDays()
    {
        String value = configService.selectConfigByKey(AFTER_SALE_VALID_DAYS_KEY);
        if (StringUtils.isEmpty(value))
        {
            return DEFAULT_AFTER_SALE_VALID_DAYS;
        }
        try
        {
            int validDays = Integer.parseInt(value);
            return validDays > 0 ? validDays : DEFAULT_AFTER_SALE_VALID_DAYS;
        }
        catch (NumberFormatException ex)
        {
            return DEFAULT_AFTER_SALE_VALID_DAYS;
        }
    }

    private void validateTicketQuantity(DigitalAfterSaleTicket ticket, DigitalSaleOrderItem orderItem)
    {
        if (TYPE_RETURN.equals(ticket.getTicketType())
                && ticketMapper.countActiveTickets(ticket.getOrderItemId()) > 0)
        {
            throw new ServiceException("订单明细存在未结束的售后工单");
        }
        if (TYPE_REPAIR.equals(ticket.getTicketType())
                && ticketMapper.countActiveReturnTickets(ticket.getOrderItemId()) > 0)
        {
            throw new ServiceException("订单明细存在未结束的退货工单");
        }

        int purchaseQuantity = defaultZero(orderItem.getQuantity());
        int refundedQuantity = defaultZero(orderItem.getRefundedQuantity());
        int waitingReturnQuantity = TYPE_RETURN.equals(ticket.getTicketType())
                ? ticketMapper.sumActiveTicketQuantity(ticket.getOrderItemId(), TYPE_RETURN) : 0;
        int unavailableQuantity = refundedQuantity + waitingReturnQuantity;
        if (ticket.getApplyQuantity() + unavailableQuantity > purchaseQuantity)
        {
            throw new ServiceException("申请数量超过可申请售后数量");
        }
    }

    private void validateReturnCheckRequest(DigitalAfterSaleTicket ticket,
            DigitalAfterSaleTicket request)
    {
        if (StringUtils.isNull(request) || StringUtils.isNull(request.getReceivedQuantity())
                || request.getReceivedQuantity() <= 0)
        {
            throw new ServiceException("实收数量必须大于0");
        }
        if (request.getReceivedQuantity() > ticket.getApplyQuantity())
        {
            throw new ServiceException("实收数量不能超过申请数量");
        }
        if (!HANDLING_RESULT_GOOD.equals(request.getHandlingResult())
                && !HANDLING_RESULT_DAMAGED.equals(request.getHandlingResult()))
        {
            throw new ServiceException("商品处理结论不正确");
        }
    }

    private String buildReturnCheckResult(DigitalAfterSaleTicket request)
    {
        String result = "退货检查完成，实收数量：" + request.getReceivedQuantity()
                + "，商品处理结论："
                + (HANDLING_RESULT_GOOD.equals(request.getHandlingResult()) ? "完好" : "损坏");
        return StringUtils.isEmpty(request.getRemark())
                ? result : result + "，备注：" + request.getRemark();
    }

    private int defaultZero(Integer value)
    {
        return StringUtils.isNull(value) ? 0 : value;
    }

    private SysUser validateHandler(Long handlerId)
    {
        if (StringUtils.isNull(handlerId))
        {
            throw new ServiceException("处理人不能为空");
        }
        SysUser handler = userService.selectUserById(handlerId);
        if (StringUtils.isNull(handler) || !USER_STATUS_NORMAL.equals(handler.getStatus()))
        {
            throw new ServiceException("处理人不存在或已停用");
        }
        return handler;
    }

    private String getDisplayName(SysUser user)
    {
        return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private String buildCreateLog(DigitalAfterSaleTicket ticket, DigitalSaleOrderItem orderItem)
    {
        return "创建" + (TYPE_RETURN.equals(ticket.getTicketType()) ? "退货" : "维修")
                + "工单，商品：" + orderItem.getProductName() + " / " + orderItem.getSkuName()
                + "，数量：" + ticket.getApplyQuantity();
    }

    private void requireActiveStatus(DigitalAfterSaleTicket ticket, String message)
    {
        if (!STATUS_PENDING.equals(ticket.getStatus()) && !STATUS_PROCESSING.equals(ticket.getStatus()))
        {
            throw new ServiceException(message);
        }
    }

    private DigitalAfterSaleTicket requireTicketForUpdate(Long ticketId)
    {
        if (StringUtils.isNull(ticketId))
        {
            throw new ServiceException("工单不能为空");
        }
        DigitalAfterSaleTicket ticket = ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(ticketId);
        if (StringUtils.isNull(ticket))
        {
            throw new ServiceException("工单不存在");
        }
        return ticket;
    }

    private DigitalAfterSaleTicket loadTicket(Long ticketId)
    {
        if (StringUtils.isNull(ticketId))
        {
            return null;
        }
        DigitalAfterSaleTicket ticket = ticketMapper.selectDigitalAfterSaleTicketById(ticketId);
        if (StringUtils.isNotNull(ticket))
        {
            ticket.setLogs(logMapper.selectDigitalAfterSaleLogList(ticketId));
        }
        return ticket;
    }

    private void writeLog(Long ticketId, String operateType, String content, String attachmentUrls,
            String operator)
    {
        DigitalAfterSaleLog log = new DigitalAfterSaleLog();
        log.setTicketId(ticketId);
        log.setOperateType(operateType);
        log.setOperateContent(content);
        log.setAttachmentUrls(attachmentUrls);
        log.setCreateBy(operator);
        if (logMapper.insertDigitalAfterSaleLog(log) != 1)
        {
            throw new ServiceException("记录售后处理日志失败");
        }
    }
}
