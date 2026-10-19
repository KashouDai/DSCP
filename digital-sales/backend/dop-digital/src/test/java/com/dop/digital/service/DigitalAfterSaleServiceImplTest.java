package com.dop.digital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dop.common.core.domain.entity.SysUser;
import com.dop.common.exception.ServiceException;
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
import com.dop.digital.service.impl.DigitalAfterSaleNoGenerator;
import com.dop.digital.service.impl.DigitalAfterSaleServiceImpl;
import com.dop.system.service.ISysConfigService;
import com.dop.system.service.ISysUserService;

/**
 * 售后工单服务测试
 *
 * @author dop
 */
@ExtendWith(MockitoExtension.class)
public class DigitalAfterSaleServiceImplTest
{
    @Mock
    private DigitalAfterSaleTicketMapper ticketMapper;

    @Mock
    private DigitalAfterSaleLogMapper logMapper;

    @Mock
    private DigitalSaleOrderMapper orderMapper;

    @Mock
    private DigitalSaleOrderItemMapper orderItemMapper;

    @Mock
    private DigitalRefundRecordMapper refundMapper;

    @Mock
    private IDigitalStockService stockService;

    @Mock
    private DigitalAfterSaleNoGenerator ticketNoGenerator;

    @Mock
    private ISysConfigService configService;

    @Mock
    private ISysUserService userService;

    @InjectMocks
    private DigitalAfterSaleServiceImpl afterSaleService;

    @Test
    public void testCreateRejectsOrderThatIsNotCompleted()
    {
        DigitalAfterSaleTicket request = request("RETURN", 100L, 1, null);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderMapper.selectDigitalSaleOrderById(10L)).thenReturn(order(10L, "CONFIRMED", new Date()));

        assertThrows(ServiceException.class,
                () -> afterSaleService.createDigitalAfterSaleTicket(request, "admin"));

        verify(ticketMapper, never()).insertDigitalAfterSaleTicket(any());
    }

    @Test
    public void testCreateRejectsReturnQuantityOverRemainingQuantity()
    {
        DigitalAfterSaleTicket request = request("RETURN", 100L, 2, null);
        DigitalSaleOrderItem item = orderItem(2);
        item.setRefundedQuantity(1);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(item);
        when(orderMapper.selectDigitalSaleOrderById(10L)).thenReturn(order(10L, "COMPLETED", new Date()));

        assertThrows(ServiceException.class,
                () -> afterSaleService.createDigitalAfterSaleTicket(request, "admin"));

        verify(ticketMapper, never()).insertDigitalAfterSaleTicket(any());
    }

    @Test
    public void testCreateRejectsRepairQuantityOverRemainingQuantity()
    {
        DigitalAfterSaleTicket request = request("REPAIR", 100L, 3, null);
        DigitalSaleOrderItem item = orderItem(3);
        item.setRefundedQuantity(1);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(item);
        when(orderMapper.selectDigitalSaleOrderById(10L)).thenReturn(order(10L, "COMPLETED", new Date()));

        assertThrows(ServiceException.class,
                () -> afterSaleService.createDigitalAfterSaleTicket(request, "admin"));

        verify(ticketMapper, never()).insertDigitalAfterSaleTicket(any());
    }

    @Test
    public void testCreateReturnRejectsExistingActiveTicket()
    {
        DigitalAfterSaleTicket request = request("RETURN", 100L, 1, null);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderMapper.selectDigitalSaleOrderById(10L)).thenReturn(order(10L, "COMPLETED", new Date()));
        when(ticketMapper.countActiveTickets(100L)).thenReturn(1);

        assertThrows(ServiceException.class,
                () -> afterSaleService.createDigitalAfterSaleTicket(request, "admin"));

        verify(ticketMapper, never()).insertDigitalAfterSaleTicket(any());
    }

    @Test
    public void testCreateRepairRejectsExistingActiveReturn()
    {
        DigitalAfterSaleTicket request = request("REPAIR", 100L, 1, null);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderMapper.selectDigitalSaleOrderById(10L)).thenReturn(order(10L, "COMPLETED", new Date()));
        when(ticketMapper.countActiveReturnTickets(100L)).thenReturn(1);

        assertThrows(ServiceException.class,
                () -> afterSaleService.createDigitalAfterSaleTicket(request, "admin"));

        verify(ticketMapper, never()).insertDigitalAfterSaleTicket(any());
    }

    @Test
    public void testCreateRepairAllowsExistingActiveRepair()
    {
        DigitalAfterSaleTicket request = request("REPAIR", 100L, 1, null);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderMapper.selectDigitalSaleOrderById(10L)).thenReturn(order(10L, "COMPLETED", new Date()));
        when(ticketNoGenerator.nextTicketNo()).thenReturn("AS202610020001");
        doAnswer(invocation -> {
            DigitalAfterSaleTicket inserted = invocation.getArgument(0);
            inserted.setTicketId(1L);
            return 1;
        }).when(ticketMapper).insertDigitalAfterSaleTicket(request);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(request);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        DigitalAfterSaleTicket result =
                afterSaleService.createDigitalAfterSaleTicket(request, "admin");

        assertSame(request, result);
        verify(ticketMapper).countActiveReturnTickets(100L);
        verify(ticketMapper, never()).countActiveTickets(100L);
        verifyLog("CREATE", "创建维修工单");
    }

    @Test
    public void testCreateRejectsExpiredOrderWithoutSourceRepair()
    {
        DigitalAfterSaleTicket request = request("RETURN", 100L, 1, null);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderMapper.selectDigitalSaleOrderById(10L))
                .thenReturn(order(10L, "COMPLETED", daysAgo(31)));
        when(configService.selectConfigByKey("digital.afterSale.validDays")).thenReturn("30");

        assertThrows(ServiceException.class,
                () -> afterSaleService.createDigitalAfterSaleTicket(request, "admin"));

        verify(ticketMapper, never()).insertDigitalAfterSaleTicket(any());
    }

    @Test
    public void testCreateReturnFromCompletedRepairExemptsValidity()
    {
        DigitalAfterSaleTicket request = request("RETURN", 100L, 1, 9L);
        DigitalAfterSaleTicket sourceTicket = new DigitalAfterSaleTicket();
        sourceTicket.setTicketId(9L);
        sourceTicket.setTicketNo("AS202609010001");
        sourceTicket.setTicketType("REPAIR");
        sourceTicket.setOrderItemId(100L);
        sourceTicket.setStatus("COMPLETED");
        sourceTicket.setHandleResult("客户要求转退货");

        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderMapper.selectDigitalSaleOrderById(10L))
                .thenReturn(order(10L, "COMPLETED", daysAgo(31)));
        when(configService.selectConfigByKey("digital.afterSale.validDays")).thenReturn("30");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(9L)).thenReturn(sourceTicket);
        when(ticketNoGenerator.nextTicketNo()).thenReturn("AS202610020002");
        doAnswer(invocation -> {
            DigitalAfterSaleTicket inserted = invocation.getArgument(0);
            inserted.setTicketId(2L);
            return 1;
        }).when(ticketMapper).insertDigitalAfterSaleTicket(request);
        when(ticketMapper.selectDigitalAfterSaleTicketById(2L)).thenReturn(request);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        DigitalAfterSaleTicket result =
                afterSaleService.createDigitalAfterSaleTicket(request, "admin");

        assertEquals("维修转退货，来源工单：AS202609010001", result.getValidityExemptReason());
        verifyLog("CREATE", "创建退货工单");
    }

    @Test
    public void testAssignWritesLog()
    {
        DigitalAfterSaleTicket ticket = ticket(1L, "REPAIR", "PENDING");
        SysUser handler = user(5L, "售后小李");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(userService.selectUserById(5L)).thenReturn(handler);
        when(ticketMapper.assignDigitalAfterSaleTicket(1L, 5L, "admin")).thenReturn(1);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(ticket);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        afterSaleService.assignDigitalAfterSaleTicket(1L, 5L, "admin");

        verifyLog("ASSIGN", "分配处理人：售后小李");
    }

    @Test
    public void testHandleWritesLog()
    {
        DigitalAfterSaleTicket ticket = ticket(1L, "REPAIR", "PENDING");
        DigitalAfterSaleTicket request = new DigitalAfterSaleTicket();
        request.setTicketId(1L);
        request.setHandleResult("已联系客户并开始检测");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(ticketMapper.handleDigitalAfterSaleTicket(request)).thenReturn(1);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(ticket);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        afterSaleService.handleDigitalAfterSaleTicket(request, "admin");

        verifyLog("HANDLE", "已联系客户并开始检测");
    }

    @Test
    public void testCompleteRepairWritesLog()
    {
        DigitalAfterSaleTicket ticket = ticket(1L, "REPAIR", "PROCESSING");
        DigitalAfterSaleTicket completed = ticket(1L, "REPAIR", "COMPLETED");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(ticketMapper.completeDigitalAfterSaleTicket(1L, "维修完成", "admin")).thenReturn(1);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(completed);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        afterSaleService.completeDigitalAfterSaleTicket(1L, "维修完成", "admin");

        verifyLog("COMPLETE", "维修完成");
    }

    @Test
    public void testReturnCheckRejectsReceivedQuantityOverApplyQuantity()
    {
        DigitalAfterSaleTicket ticket = returnTicket(1L, "PENDING", 2);
        DigitalAfterSaleTicket request = returnCheckRequest(3, "GOOD");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);

        assertThrows(ServiceException.class,
                () -> afterSaleService.checkReturnDigitalAfterSaleTicket(1L, request, "admin"));

        verifyNoInteractions(refundMapper, stockService, orderItemMapper);
        verify(ticketMapper, never()).completeReturnDigitalAfterSaleTicket(
                any(), any(), any(), any(), any());
    }

    @Test
    public void testReturnCheckGoodAddsStockAndCreatesPendingRefund()
    {
        DigitalAfterSaleTicket ticket = returnTicket(1L, "PENDING", 2);
        DigitalAfterSaleTicket completed = returnTicket(1L, "COMPLETED", 2);
        DigitalAfterSaleTicket request = returnCheckRequest(2, "GOOD");
        request.setRemark("外观完好");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderItemMapper.increaseRefundedQuantity(100L, 2, "admin")).thenReturn(1);
        doAnswer(invocation -> {
            DigitalRefundRecord refund = invocation.getArgument(0);
            refund.setRefundId(7L);
            return 1;
        }).when(refundMapper).insertDigitalRefundRecord(any());
        when(ticketMapper.completeReturnDigitalAfterSaleTicket(
                1L, 2, "GOOD", "退货检查完成，实收数量：2，商品处理结论：完好，备注：外观完好",
                "admin")).thenReturn(1);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(completed);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        DigitalAfterSaleTicket result =
                afterSaleService.checkReturnDigitalAfterSaleTicket(1L, request, "admin");

        assertSame(completed, result);
        ArgumentCaptor<DigitalRefundRecord> refundCaptor =
                ArgumentCaptor.forClass(DigitalRefundRecord.class);
        verify(refundMapper).insertDigitalRefundRecord(refundCaptor.capture());
        assertEquals(1L, refundCaptor.getValue().getTicketId());
        assertEquals("PENDING", refundCaptor.getValue().getStatus());
        assertEquals(BigDecimal.ZERO, refundCaptor.getValue().getRefundAmount());
        verify(stockService).returnInbound(1000L, 2, 1L, "admin", "退货工单：AS202610020001");
        verifyLog("RETURN_CHECK", "退货检查完成，实收数量：2");
    }

    @Test
    public void testReturnCheckDamagedDoesNotAddStock()
    {
        DigitalAfterSaleTicket ticket = returnTicket(1L, "PENDING", 2);
        DigitalAfterSaleTicket completed = returnTicket(1L, "COMPLETED", 2);
        DigitalAfterSaleTicket request = returnCheckRequest(2, "DAMAGED");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L)).thenReturn(orderItem(5));
        when(orderItemMapper.increaseRefundedQuantity(100L, 2, "admin")).thenReturn(1);
        when(refundMapper.insertDigitalRefundRecord(any())).thenReturn(1);
        when(ticketMapper.completeReturnDigitalAfterSaleTicket(
                1L, 2, "DAMAGED", "退货检查完成，实收数量：2，商品处理结论：损坏",
                "admin")).thenReturn(1);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(completed);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        afterSaleService.checkReturnDigitalAfterSaleTicket(1L, request, "admin");

        verifyNoInteractions(stockService);
        verifyLog("RETURN_CHECK", "退货检查完成，实收数量：2，商品处理结论：损坏");
    }

    @Test
    public void testReturnCheckIsIdempotentWhenTicketAlreadyCompleted()
    {
        DigitalAfterSaleTicket ticket = returnTicket(1L, "COMPLETED", 2);
        DigitalAfterSaleTicket request = returnCheckRequest(2, "GOOD");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(ticket);

        DigitalAfterSaleTicket result =
                afterSaleService.checkReturnDigitalAfterSaleTicket(1L, request, "admin");

        assertSame(ticket, result);
        verifyNoInteractions(refundMapper, stockService, orderItemMapper);
        verify(ticketMapper, never()).completeReturnDigitalAfterSaleTicket(
                any(), any(), any(), any(), any());
        verify(logMapper, never()).insertDigitalAfterSaleLog(any());
    }

    @Test
    public void testCloseWritesLog()
    {
        DigitalAfterSaleTicket ticket = ticket(1L, "RETURN", "PENDING");
        DigitalAfterSaleTicket closed = ticket(1L, "RETURN", "CLOSED");
        when(ticketMapper.selectDigitalAfterSaleTicketByIdForUpdate(1L)).thenReturn(ticket);
        when(ticketMapper.closeDigitalAfterSaleTicket(1L, "客户取消申请", "admin")).thenReturn(1);
        when(ticketMapper.selectDigitalAfterSaleTicketById(1L)).thenReturn(closed);
        when(logMapper.insertDigitalAfterSaleLog(any())).thenReturn(1);

        afterSaleService.closeDigitalAfterSaleTicket(1L, "客户取消申请", "admin");

        verifyLog("CLOSE", "客户取消申请");
    }

    private DigitalAfterSaleTicket request(String ticketType, Long orderItemId,
            Integer applyQuantity, Long sourceTicketId)
    {
        DigitalAfterSaleTicket ticket = new DigitalAfterSaleTicket();
        ticket.setTicketType(ticketType);
        ticket.setOrderItemId(orderItemId);
        ticket.setApplyQuantity(applyQuantity);
        ticket.setSourceTicketId(sourceTicketId);
        return ticket;
    }

    private DigitalSaleOrder order(Long orderId, String status, Date completeTime)
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setOrderId(orderId);
        order.setOrderNo("SO202610020001");
        order.setStatus(status);
        order.setCompleteTime(completeTime);
        return order;
    }

    private DigitalSaleOrderItem orderItem(Integer quantity)
    {
        DigitalSaleOrderItem item = new DigitalSaleOrderItem();
        item.setItemId(100L);
        item.setOrderId(10L);
        item.setSkuId(1000L);
        item.setProductName("65W充电器");
        item.setSkuName("黑色");
        item.setSalePrice(new BigDecimal("65.00"));
        item.setQuantity(quantity);
        item.setRefundedQuantity(0);
        return item;
    }

    private DigitalAfterSaleTicket ticket(Long ticketId, String ticketType, String status)
    {
        DigitalAfterSaleTicket ticket = new DigitalAfterSaleTicket();
        ticket.setTicketId(ticketId);
        ticket.setTicketType(ticketType);
        ticket.setStatus(status);
        return ticket;
    }

    private DigitalAfterSaleTicket returnTicket(Long ticketId, String status, Integer applyQuantity)
    {
        DigitalAfterSaleTicket ticket = ticket(ticketId, "RETURN", status);
        ticket.setTicketNo("AS202610020001");
        ticket.setOrderItemId(100L);
        ticket.setApplyQuantity(applyQuantity);
        return ticket;
    }

    private DigitalAfterSaleTicket returnCheckRequest(Integer receivedQuantity,
            String handlingResult)
    {
        DigitalAfterSaleTicket request = new DigitalAfterSaleTicket();
        request.setReceivedQuantity(receivedQuantity);
        request.setHandlingResult(handlingResult);
        return request;
    }

    private SysUser user(Long userId, String nickName)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("handler");
        user.setNickName(nickName);
        user.setStatus("0");
        return user;
    }

    private Date daysAgo(int days)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        return calendar.getTime();
    }

    private void verifyLog(String operateType, String contentPrefix)
    {
        ArgumentCaptor<DigitalAfterSaleLog> logCaptor =
                ArgumentCaptor.forClass(DigitalAfterSaleLog.class);
        verify(logMapper).insertDigitalAfterSaleLog(logCaptor.capture());
        assertEquals(operateType, logCaptor.getValue().getOperateType());
        assertEquals(true, logCaptor.getValue().getOperateContent().startsWith(contentPrefix));
    }
}
