package com.dop.digital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dop.common.exception.ServiceException;
import com.dop.digital.domain.DigitalCustomer;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.domain.DigitalSaleOrder;
import com.dop.digital.domain.DigitalSaleOrderItem;
import com.dop.digital.domain.DigitalStockRecord;
import com.dop.digital.mapper.DigitalCustomerMapper;
import com.dop.digital.mapper.DigitalProductSkuMapper;
import com.dop.digital.mapper.DigitalSaleOrderItemMapper;
import com.dop.digital.mapper.DigitalSaleOrderMapper;
import com.dop.digital.mapper.DigitalStockRecordMapper;
import com.dop.digital.service.impl.DigitalOrderNoGenerator;
import com.dop.digital.service.impl.DigitalSaleOrderServiceImpl;

/**
 * 销售订单服务测试
 *
 * @author dop
 */
@ExtendWith(MockitoExtension.class)
public class DigitalSaleOrderServiceImplTest
{
    @Mock
    private DigitalSaleOrderMapper orderMapper;

    @Mock
    private DigitalSaleOrderItemMapper orderItemMapper;

    @Mock
    private DigitalCustomerMapper customerMapper;

    @Mock
    private DigitalProductSkuMapper skuMapper;

    @Mock
    private DigitalStockRecordMapper stockRecordMapper;

    @Mock
    private DigitalOrderNoGenerator orderNoGenerator;

    @InjectMocks
    private DigitalSaleOrderServiceImpl orderService;

    @Test
    public void testCreateDraftKeepsSnapshotAndDoesNotChangeStock()
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setRequestNo("REQ-ORDER-001");
        order.setCustomerId(10L);
        order.setItems(List.of(orderItem(100L, 1, "65.00"), orderItem(100L, 2, "65.00")));

        when(customerMapper.selectDigitalCustomerById(10L)).thenReturn(customer(10L));
        when(skuMapper.selectDigitalProductSkuById(100L)).thenReturn(sku(100L, "65W充电器", "黑色", 30));
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202610020001");
        doAnswer(invocation -> {
            DigitalSaleOrder inserted = invocation.getArgument(0);
            inserted.setOrderId(1L);
            return 1;
        }).when(orderMapper).insertDigitalSaleOrder(order);
        when(orderItemMapper.batchInsertDigitalSaleOrderItem(any())).thenReturn(1);

        DigitalSaleOrder created = new DigitalSaleOrder();
        created.setOrderId(1L);
        created.setOrderNo("SO202610020001");
        created.setStatus("DRAFT");
        when(orderMapper.selectDigitalSaleOrderById(1L)).thenReturn(created);
        when(orderItemMapper.selectDigitalSaleOrderItemList(1L)).thenReturn(order.getItems());

        DigitalSaleOrder result = orderService.createDigitalSaleOrderDraft(order, "admin");

        assertSame(created, result);
        assertEquals(new BigDecimal("195.00"), order.getTotalAmount());
        assertEquals(1, order.getItems().size());
        assertEquals(3, order.getItems().get(0).getQuantity());
        assertEquals("65W充电器", order.getItems().get(0).getProductName());
        assertEquals("黑色", order.getItems().get(0).getSkuName());
        assertEquals(new BigDecimal("65.00"), order.getItems().get(0).getSalePrice());
        verify(skuMapper, never()).decreaseStock(any(), any(), any());
        verify(skuMapper, never()).increaseStock(any(), any(), any());
    }

    @Test
    public void testCreateDraftRejectsDifferentPricesForSameSku()
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setRequestNo("REQ-ORDER-002");
        order.setCustomerId(10L);
        order.setItems(List.of(orderItem(100L, 1, "65.00"), orderItem(100L, 1, "66.00")));

        when(customerMapper.selectDigitalCustomerById(10L)).thenReturn(customer(10L));
        when(skuMapper.selectDigitalProductSkuById(100L)).thenReturn(sku(100L, "65W充电器", "黑色", 30));

        assertThrows(ServiceException.class,
                () -> orderService.createDigitalSaleOrderDraft(order, "admin"));
        verify(orderMapper, never()).insertDigitalSaleOrder(any());
    }

    @Test
    public void testCreateDraftWithSameRequestNoReturnsExistingOrder()
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setRequestNo("REQ-ORDER-IDEMPOTENT");
        order.setCustomerId(10L);
        order.setItems(List.of(orderItem(100L, 1, "65.00")));

        when(customerMapper.selectDigitalCustomerById(10L)).thenReturn(customer(10L));
        when(skuMapper.selectDigitalProductSkuById(100L))
                .thenReturn(sku(100L, "65W充电器", "黑色", 30));
        when(orderNoGenerator.nextOrderNo()).thenReturn("SO202610020002");
        doAnswer(invocation -> {
            DigitalSaleOrder inserted = invocation.getArgument(0);
            inserted.setOrderId(2L);
            return 1;
        }).when(orderMapper).insertDigitalSaleOrder(order);
        when(orderItemMapper.batchInsertDigitalSaleOrderItem(any())).thenReturn(1);

        DigitalSaleOrder created = order(2L, "DRAFT");
        when(orderMapper.selectDigitalSaleOrderById(2L)).thenReturn(created);
        when(orderItemMapper.selectDigitalSaleOrderItemList(2L)).thenReturn(order.getItems());

        DigitalSaleOrder first = orderService.createDigitalSaleOrderDraft(order, "admin");

        DigitalSaleOrder existing = order(2L, "DRAFT");
        existing.setRequestHash(order.getRequestHash());
        when(orderMapper.selectDigitalSaleOrderByRequestNo("REQ-ORDER-IDEMPOTENT"))
                .thenReturn(existing);
        when(orderMapper.selectDigitalSaleOrderById(2L)).thenReturn(existing);

        DigitalSaleOrder second = orderService.createDigitalSaleOrderDraft(order, "admin");

        assertSame(first, created);
        assertSame(existing, second);
        verify(orderMapper).insertDigitalSaleOrder(order);
    }

    @Test
    public void testCreateDraftRejectsSameRequestNoWithDifferentContent()
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setRequestNo("REQ-ORDER-CONFLICT");
        order.setCustomerId(10L);
        order.setItems(List.of(orderItem(100L, 1, "65.00")));

        when(customerMapper.selectDigitalCustomerById(10L)).thenReturn(customer(10L));
        when(skuMapper.selectDigitalProductSkuById(100L))
                .thenReturn(sku(100L, "65W充电器", "黑色", 30));
        DigitalSaleOrder existing = order(3L, "DRAFT");
        existing.setRequestHash("different-request-hash");
        when(orderMapper.selectDigitalSaleOrderByRequestNo("REQ-ORDER-CONFLICT"))
                .thenReturn(existing);

        assertThrows(ServiceException.class,
                () -> orderService.createDigitalSaleOrderDraft(order, "admin"));
        verify(orderMapper, never()).insertDigitalSaleOrder(any());
    }

    @Test
    public void testCreateDraftRequiresRequestNo()
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setCustomerId(10L);
        order.setItems(List.of(orderItem(100L, 1, "65.00")));

        assertThrows(ServiceException.class,
                () -> orderService.createDigitalSaleOrderDraft(order, "admin"));
        verify(orderMapper, never()).insertDigitalSaleOrder(any());
    }

    @Test
    public void testConfirmDecreasesStockAndWritesSaleRecord()
    {
        DigitalSaleOrder draft = order(1L, "DRAFT");
        DigitalSaleOrderItem item = orderItem(100L, 2, "65.00");
        item.setOrderId(1L);
        when(orderMapper.selectDigitalSaleOrderByIdForUpdate(1L)).thenReturn(draft);
        when(orderItemMapper.selectDigitalSaleOrderItemList(1L)).thenReturn(List.of(item));
        when(orderMapper.confirmDigitalSaleOrder(1L, "admin")).thenReturn(1);
        when(skuMapper.selectDigitalProductSkuById(100L))
                .thenReturn(sku(100L, "65W充电器", "黑色", 30), sku(100L, "65W充电器", "黑色", 28));
        when(skuMapper.decreaseStock(100L, 2, "admin")).thenReturn(1);
        when(orderMapper.selectDigitalSaleOrderById(1L)).thenReturn(order(1L, "CONFIRMED"));

        DigitalSaleOrder result = orderService.confirmDigitalSaleOrder(1L, "admin");

        assertEquals("CONFIRMED", result.getStatus());
        verify(skuMapper).decreaseStock(100L, 2, "admin");
        ArgumentCaptor<DigitalStockRecord> recordCaptor = ArgumentCaptor.forClass(DigitalStockRecord.class);
        verify(stockRecordMapper).insertDigitalStockRecord(recordCaptor.capture());
        assertEquals("SALE_OUT", recordCaptor.getValue().getRecordType());
        assertEquals(-1, recordCaptor.getValue().getChangeDirection());
        assertEquals(30, recordCaptor.getValue().getBeforeStock());
        assertEquals(28, recordCaptor.getValue().getAfterStock());
        assertEquals(1L, recordCaptor.getValue().getBizId());
    }

    @Test
    public void testConfirmConfirmedOrderIsIdempotent()
    {
        DigitalSaleOrder confirmed = order(1L, "CONFIRMED");
        when(orderMapper.selectDigitalSaleOrderByIdForUpdate(1L)).thenReturn(confirmed);

        DigitalSaleOrder result = orderService.confirmDigitalSaleOrder(1L, "admin");

        assertSame(confirmed, result);
        verify(orderMapper, never()).confirmDigitalSaleOrder(any(), any());
        verify(skuMapper, never()).decreaseStock(any(), any(), any());
        verify(stockRecordMapper, never()).insertDigitalStockRecord(any());
    }

    @Test
    public void testCancelConfirmedOrderRestoresStock()
    {
        DigitalSaleOrder confirmed = order(1L, "CONFIRMED");
        DigitalSaleOrderItem item = orderItem(100L, 2, "65.00");
        item.setOrderId(1L);
        when(orderMapper.selectDigitalSaleOrderByIdForUpdate(1L)).thenReturn(confirmed);
        when(orderItemMapper.selectDigitalSaleOrderItemList(1L)).thenReturn(List.of(item));
        when(orderMapper.cancelDigitalSaleOrderConfirmed(1L, "admin")).thenReturn(1);
        when(skuMapper.increaseStock(100L, 2, "admin")).thenReturn(1);
        when(skuMapper.selectDigitalProductSkuById(100L))
                .thenReturn(sku(100L, "65W充电器", "黑色", 30));
        when(orderMapper.selectDigitalSaleOrderById(1L)).thenReturn(order(1L, "CANCELLED"));

        DigitalSaleOrder result = orderService.cancelDigitalSaleOrder(1L, "admin");

        assertEquals("CANCELLED", result.getStatus());
        verify(skuMapper).increaseStock(100L, 2, "admin");
        ArgumentCaptor<DigitalStockRecord> recordCaptor = ArgumentCaptor.forClass(DigitalStockRecord.class);
        verify(stockRecordMapper).insertDigitalStockRecord(recordCaptor.capture());
        assertEquals("CANCEL_IN", recordCaptor.getValue().getRecordType());
        assertEquals(1, recordCaptor.getValue().getChangeDirection());
        assertEquals(28, recordCaptor.getValue().getBeforeStock());
        assertEquals(30, recordCaptor.getValue().getAfterStock());
    }

    @Test
    public void testCancelCompletedOrderIsRejected()
    {
        DigitalSaleOrder completed = order(1L, "COMPLETED");
        when(orderMapper.selectDigitalSaleOrderByIdForUpdate(1L)).thenReturn(completed);

        assertThrows(ServiceException.class, () -> orderService.cancelDigitalSaleOrder(1L, "admin"));
        verify(orderMapper, never()).cancelDigitalSaleOrderConfirmed(any(), any());
        verify(skuMapper, never()).increaseStock(any(), any(), any());
    }

    @Test
    public void testCompleteConfirmedOrderDoesNotChangeStock()
    {
        DigitalSaleOrder confirmed = order(1L, "CONFIRMED");
        when(orderMapper.selectDigitalSaleOrderByIdForUpdate(1L)).thenReturn(confirmed);
        when(orderMapper.completeDigitalSaleOrder(1L, "admin")).thenReturn(1);
        when(orderMapper.selectDigitalSaleOrderById(1L)).thenReturn(order(1L, "COMPLETED"));

        DigitalSaleOrder result = orderService.completeDigitalSaleOrder(1L, "admin");

        assertEquals("COMPLETED", result.getStatus());
        verify(skuMapper, never()).decreaseStock(any(), any(), any());
        verify(skuMapper, never()).increaseStock(any(), any(), any());
    }

    private DigitalCustomer customer(Long customerId)
    {
        DigitalCustomer customer = new DigitalCustomer();
        customer.setCustomerId(customerId);
        customer.setCustomerName("测试客户");
        customer.setPhone("13800000000");
        customer.setStatus("0");
        return customer;
    }

    private DigitalProductSku sku(Long skuId, String productName, String skuName, Integer stock)
    {
        DigitalProductSku sku = new DigitalProductSku();
        sku.setSkuId(skuId);
        sku.setProductName(productName);
        sku.setSkuName(skuName);
        sku.setSalePrice(new BigDecimal("79.00"));
        sku.setStock(stock);
        return sku;
    }

    private DigitalSaleOrder order(Long orderId, String status)
    {
        DigitalSaleOrder order = new DigitalSaleOrder();
        order.setOrderId(orderId);
        order.setOrderNo("SO202610020001");
        order.setStatus(status);
        return order;
    }

    private DigitalSaleOrderItem orderItem(Long skuId, Integer quantity, String salePrice)
    {
        DigitalSaleOrderItem item = new DigitalSaleOrderItem();
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setSalePrice(new BigDecimal(salePrice));
        return item;
    }
}
