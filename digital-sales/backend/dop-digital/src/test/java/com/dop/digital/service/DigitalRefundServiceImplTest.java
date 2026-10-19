package com.dop.digital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dop.common.exception.ServiceException;
import com.dop.digital.domain.DigitalRefundRecord;
import com.dop.digital.domain.DigitalSaleOrderItem;
import com.dop.digital.mapper.DigitalRefundRecordMapper;
import com.dop.digital.mapper.DigitalSaleOrderItemMapper;
import com.dop.digital.service.impl.DigitalRefundServiceImpl;

/**
 * 退款记录服务测试
 *
 * @author dop
 */
@ExtendWith(MockitoExtension.class)
public class DigitalRefundServiceImplTest
{
    @Mock
    private DigitalRefundRecordMapper refundMapper;

    @Mock
    private DigitalSaleOrderItemMapper orderItemMapper;

    @InjectMocks
    private DigitalRefundServiceImpl refundService;

    @Test
    public void testAuditRejectsAmountAboveReceivedQuantityLimit()
    {
        DigitalRefundRecord pending = refund("PENDING");
        DigitalRefundRecord loaded = loadedRefund("PENDING", "65.00", 2);
        DigitalRefundRecord request = auditRequest("130.01");
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(pending);
        when(refundMapper.selectDigitalRefundRecordById(7L)).thenReturn(loaded);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L))
                .thenReturn(orderItem(5, "0.00"));

        assertThrows(ServiceException.class,
                () -> refundService.auditDigitalRefund(7L, request, 5L, "admin"));

        verify(refundMapper, never()).auditDigitalRefund(any(), any(), any(), any());
        verify(orderItemMapper, never()).increaseRefundedAmount(any(), any(), any());
    }

    @Test
    public void testAuditRejectsZeroAmount()
    {
        DigitalRefundRecord pending = refund("PENDING");
        DigitalRefundRecord loaded = loadedRefund("PENDING", "65.00", 1);
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(pending);
        when(refundMapper.selectDigitalRefundRecordById(7L)).thenReturn(loaded);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L))
                .thenReturn(orderItem(1, "0.00"));

        assertThrows(ServiceException.class,
                () -> refundService.auditDigitalRefund(7L, auditRequest("0.00"), 5L, "admin"));

        verify(refundMapper, never()).auditDigitalRefund(any(), any(), any(), any());
        verify(orderItemMapper, never()).increaseRefundedAmount(any(), any(), any());
    }

    @Test
    public void testAuditRejectsAmountAboveRemainingDetailAmount()
    {
        DigitalRefundRecord pending = refund("PENDING");
        DigitalRefundRecord loaded = loadedRefund("PENDING", "65.00", 2);
        DigitalRefundRecord request = auditRequest("75.01");
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(pending);
        when(refundMapper.selectDigitalRefundRecordById(7L)).thenReturn(loaded);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L))
                .thenReturn(orderItem(5, "250.00"));

        assertThrows(ServiceException.class,
                () -> refundService.auditDigitalRefund(7L, request, 5L, "admin"));

        verify(refundMapper, never()).auditDigitalRefund(any(), any(), any(), any());
        verify(orderItemMapper, never()).increaseRefundedAmount(any(), any(), any());
    }

    @Test
    public void testAuditUsesSmallerRemainingDetailAmount()
    {
        DigitalRefundRecord pending = refund("PENDING");
        DigitalRefundRecord loaded = loadedRefund("PENDING", "65.00", 2);
        DigitalRefundRecord refunded = loadedRefund("REFUNDED", "65.00", 2);
        DigitalRefundRecord request = auditRequest("75.00");
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(pending);
        when(refundMapper.selectDigitalRefundRecordById(7L))
                .thenReturn(loaded, refunded);
        when(orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(100L))
                .thenReturn(orderItem(5, "250.00"));
        when(refundMapper.auditDigitalRefund(7L, new BigDecimal("75.00"), 5L, "admin"))
                .thenReturn(1);
        when(orderItemMapper.increaseRefundedAmount(100L, new BigDecimal("75.00"), "admin"))
                .thenReturn(1);

        DigitalRefundRecord result =
                refundService.auditDigitalRefund(7L, request, 5L, "admin");

        assertSame(refunded, result);
        verify(refundMapper).auditDigitalRefund(7L, new BigDecimal("75.00"), 5L, "admin");
        verify(orderItemMapper).increaseRefundedAmount(100L, new BigDecimal("75.00"), "admin");
    }

    @Test
    public void testAuditIsIdempotentForRefundedRecord()
    {
        DigitalRefundRecord refunded = refund("REFUNDED");
        DigitalRefundRecord loaded = loadedRefund("REFUNDED", "65.00", 2);
        DigitalRefundRecord request = auditRequest("65.00");
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(refunded);
        when(refundMapper.selectDigitalRefundRecordById(7L)).thenReturn(loaded);

        DigitalRefundRecord result =
                refundService.auditDigitalRefund(7L, request, 5L, "admin");

        assertSame(loaded, result);
        verify(refundMapper, never()).auditDigitalRefund(any(), any(), any(), any());
        verifyNoInteractions(orderItemMapper);
    }

    @Test
    public void testCancelPendingRefund()
    {
        DigitalRefundRecord pending = refund("PENDING");
        DigitalRefundRecord cancelled = refund("CANCELLED");
        DigitalRefundRecord request = new DigitalRefundRecord();
        request.setRemark("客户撤销退款");
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(pending);
        when(refundMapper.cancelDigitalRefund(7L, "客户撤销退款", 5L, "admin")).thenReturn(1);
        when(refundMapper.selectDigitalRefundRecordById(7L)).thenReturn(cancelled);

        DigitalRefundRecord result =
                refundService.cancelDigitalRefund(7L, request, 5L, "admin");

        assertSame(cancelled, result);
        verify(refundMapper).cancelDigitalRefund(7L, "客户撤销退款", 5L, "admin");
    }

    @Test
    public void testCancelIsIdempotentForCancelledRecord()
    {
        DigitalRefundRecord cancelled = refund("CANCELLED");
        DigitalRefundRecord request = new DigitalRefundRecord();
        request.setRemark("客户撤销退款");
        when(refundMapper.selectDigitalRefundRecordByIdForUpdate(7L)).thenReturn(cancelled);
        when(refundMapper.selectDigitalRefundRecordById(7L)).thenReturn(cancelled);

        DigitalRefundRecord result =
                refundService.cancelDigitalRefund(7L, request, 5L, "admin");

        assertSame(cancelled, result);
        verify(refundMapper, never()).cancelDigitalRefund(any(), any(), any(), any());
    }

    private DigitalRefundRecord refund(String status)
    {
        DigitalRefundRecord refund = new DigitalRefundRecord();
        refund.setRefundId(7L);
        refund.setTicketId(1L);
        refund.setOrderItemId(100L);
        refund.setStatus(status);
        return refund;
    }

    private DigitalRefundRecord loadedRefund(String status, String salePrice,
            Integer receivedQuantity)
    {
        DigitalRefundRecord refund = refund(status);
        refund.setSalePrice(new BigDecimal(salePrice));
        refund.setReceivedQuantity(receivedQuantity);
        return refund;
    }

    private DigitalRefundRecord auditRequest(String amount)
    {
        DigitalRefundRecord request = new DigitalRefundRecord();
        request.setRefundAmount(new BigDecimal(amount));
        return request;
    }

    private DigitalSaleOrderItem orderItem(Integer quantity, String refundedAmount)
    {
        DigitalSaleOrderItem item = new DigitalSaleOrderItem();
        item.setItemId(100L);
        item.setQuantity(quantity);
        item.setRefundedAmount(new BigDecimal(refundedAmount));
        return item;
    }
}
