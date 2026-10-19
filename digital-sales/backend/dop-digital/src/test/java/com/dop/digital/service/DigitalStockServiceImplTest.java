package com.dop.digital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dop.common.exception.ServiceException;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.domain.DigitalStockInboundRequest;
import com.dop.digital.domain.DigitalStockRecord;
import com.dop.digital.mapper.DigitalProductSkuMapper;
import com.dop.digital.mapper.DigitalStockRecordMapper;
import com.dop.digital.service.impl.DigitalStockServiceImpl;

/**
 * 手工入库服务测试
 *
 * @author dop
 */
@ExtendWith(MockitoExtension.class)
public class DigitalStockServiceImplTest
{
    @Mock
    private DigitalProductSkuMapper skuMapper;

    @Mock
    private DigitalStockRecordMapper stockRecordMapper;

    @InjectMocks
    private DigitalStockServiceImpl stockService;

    @Test
    public void testInboundRejectsNonPositiveQuantity()
    {
        DigitalStockInboundRequest request = new DigitalStockInboundRequest();
        request.setSkuId(100L);
        request.setQuantity(0);
        request.setRequestNo("REQ-001");

        assertThrows(ServiceException.class, () -> stockService.inbound(request, "admin"));
        verify(skuMapper, never()).increaseStock(any(), any(), any());
    }

    @Test
    public void testInboundIncreasesStockAndWritesRecord()
    {
        DigitalStockInboundRequest request = new DigitalStockInboundRequest();
        request.setSkuId(100L);
        request.setQuantity(30);
        request.setRequestNo("REQ-002");
        request.setRemark("首批入库");

        DigitalProductSku sku = new DigitalProductSku();
        sku.setSkuId(100L);
        DigitalProductSku updatedSku = new DigitalProductSku();
        updatedSku.setSkuId(100L);
        updatedSku.setStock(30);

        when(stockRecordMapper.selectDigitalStockRecordByRequestNo("REQ-002")).thenReturn(null);
        when(skuMapper.selectDigitalProductSkuById(100L)).thenReturn(sku, updatedSku);
        when(skuMapper.increaseStock(100L, 30, "admin")).thenReturn(1);

        DigitalStockRecord record = stockService.inbound(request, "admin");

        assertEquals(0, record.getBeforeStock());
        assertEquals(30, record.getAfterStock());
        assertEquals("IN", record.getRecordType());
        assertEquals("MANUAL_IN", record.getBizType());
        verify(stockRecordMapper).insertDigitalStockRecord(record);
    }

    @Test
    public void testInboundReturnsExistingRecordForRepeatedRequestNo()
    {
        DigitalStockInboundRequest request = new DigitalStockInboundRequest();
        request.setSkuId(100L);
        request.setQuantity(30);
        request.setRequestNo("REQ-003");

        DigitalStockRecord existing = new DigitalStockRecord();
        existing.setRecordId(1L);
        existing.setRequestNo("REQ-003");
        when(stockRecordMapper.selectDigitalStockRecordByRequestNo("REQ-003")).thenReturn(existing);

        DigitalStockRecord result = stockService.inbound(request, "admin");

        assertSame(existing, result);
        verify(skuMapper, never()).increaseStock(any(), any(), any());
        verify(stockRecordMapper, never()).insertDigitalStockRecord(any());
    }

    @Test
    public void testOutboundDecreasesStockAndWritesRecord()
    {
        DigitalStockInboundRequest request = new DigitalStockInboundRequest();
        request.setSkuId(100L);
        request.setQuantity(3);
        request.setRequestNo("OUT-001");
        request.setRemark("盘点损耗");
        DigitalProductSku sku = new DigitalProductSku();
        sku.setSkuId(100L);
        sku.setStock(7);
        DigitalProductSku updated = new DigitalProductSku();
        updated.setSkuId(100L);
        updated.setStock(4);
        when(skuMapper.selectDigitalProductSkuById(100L)).thenReturn(sku, updated);
        when(skuMapper.decreaseStock(100L, 3, "admin")).thenReturn(1);
        when(stockRecordMapper.insertDigitalStockRecord(any())).thenReturn(1);

        DigitalStockRecord record = stockService.outbound(request, "admin");

        assertEquals(7, record.getBeforeStock());
        assertEquals(4, record.getAfterStock());
        assertEquals("OUT", record.getRecordType());
        assertEquals(-1, record.getChangeDirection());
        verify(stockRecordMapper).insertDigitalStockRecord(record);
    }

    @Test
    public void testOutboundRejectsInsufficientStock()
    {
        DigitalStockInboundRequest request = new DigitalStockInboundRequest();
        request.setSkuId(100L);
        request.setQuantity(8);
        request.setRequestNo("OUT-002");
        request.setRemark("盘点损耗");
        DigitalProductSku sku = new DigitalProductSku();
        sku.setSkuId(100L);
        when(skuMapper.selectDigitalProductSkuById(100L)).thenReturn(sku);

        assertThrows(ServiceException.class, () -> stockService.outbound(request, "admin"));
        verify(stockRecordMapper, never()).insertDigitalStockRecord(any());
    }
}
