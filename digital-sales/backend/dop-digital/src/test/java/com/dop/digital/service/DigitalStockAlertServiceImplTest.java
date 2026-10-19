package com.dop.digital.service;

import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.dop.digital.domain.DigitalStockAlert;
import com.dop.digital.mapper.DigitalStockAlertMapper;
import com.dop.digital.service.impl.DigitalStockAlertServiceImpl;

class DigitalStockAlertServiceImplTest
{
    private final DigitalStockAlertMapper mapper = mock(DigitalStockAlertMapper.class);
    private final IDigitalStockAlertService service = new DigitalStockAlertServiceImpl(mapper);

    @Test
    void testLowStockOnlyCreatesOneActiveAlert()
    {
        DigitalStockAlert alert = alert(9005L);
        when(mapper.selectLowStockSkus()).thenReturn(List.of(alert));
        when(mapper.selectActiveSkuIds()).thenReturn(List.of(), List.of(9005L));

        service.scan();
        service.scan();

        verify(mapper).insertStockAlert(alert);
    }

    @Test
    void testRecoveredStockResolvesActiveAlert()
    {
        when(mapper.selectLowStockSkus()).thenReturn(List.of());
        when(mapper.selectActiveSkuIds()).thenReturn(List.of(9005L));

        service.scan();

        verify(mapper).resolveStockAlert(9005L);
    }

    private DigitalStockAlert alert(Long skuId)
    {
        DigitalStockAlert alert = new DigitalStockAlert();
        alert.setSkuId(skuId);
        alert.setSkuCode("DEMO-PB20-WHITE");
        alert.setSkuName("白色");
        alert.setStock(4);
        alert.setWarnStock(5);
        return alert;
    }
}
