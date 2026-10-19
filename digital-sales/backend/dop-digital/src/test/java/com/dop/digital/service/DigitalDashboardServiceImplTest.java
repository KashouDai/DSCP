package com.dop.digital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.dop.digital.domain.DigitalDashboardOverview;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.mapper.DigitalDashboardMapper;
import com.dop.digital.service.impl.DigitalDashboardServiceImpl;

/**
 * 首页看板服务测试
 *
 * @author dop
 */
@ExtendWith(MockitoExtension.class)
public class DigitalDashboardServiceImplTest
{
    @Mock
    private DigitalDashboardMapper dashboardMapper;

    @InjectMocks
    private DigitalDashboardServiceImpl dashboardService;

    @Test
    public void testOverviewAggregatesMetrics()
    {
        DigitalProductSku lowStockSku = new DigitalProductSku();
        lowStockSku.setSkuId(9001L);
        List<DigitalProductSku> lowStockList = Collections.singletonList(lowStockSku);
        when(dashboardMapper.selectTodayOrderCount()).thenReturn(3L);
        when(dashboardMapper.selectTodaySalesAmount()).thenReturn(new BigDecimal("598.00"));
        when(dashboardMapper.selectTodayRefundAmount()).thenReturn(new BigDecimal("129.00"));
        when(dashboardMapper.selectPendingTicketCount()).thenReturn(2L);
        when(dashboardMapper.selectLowStockList()).thenReturn(lowStockList);

        DigitalDashboardOverview overview = dashboardService.selectDashboardOverview();

        assertEquals(3L, overview.getTodayOrderCount());
        assertEquals(new BigDecimal("598.00"), overview.getTodaySalesAmount());
        assertEquals(new BigDecimal("129.00"), overview.getTodayRefundAmount());
        assertEquals(2L, overview.getPendingTicketCount());
        assertSame(lowStockList, overview.getLowStockList());
    }

    @Test
    public void testOverviewNormalizesEmptyValues()
    {
        when(dashboardMapper.selectTodayOrderCount()).thenReturn(null);
        when(dashboardMapper.selectTodaySalesAmount()).thenReturn(null);
        when(dashboardMapper.selectTodayRefundAmount()).thenReturn(null);
        when(dashboardMapper.selectPendingTicketCount()).thenReturn(null);
        when(dashboardMapper.selectLowStockList()).thenReturn(null);

        DigitalDashboardOverview overview = dashboardService.selectDashboardOverview();

        assertEquals(0L, overview.getTodayOrderCount());
        assertEquals(BigDecimal.ZERO, overview.getTodaySalesAmount());
        assertEquals(BigDecimal.ZERO, overview.getTodayRefundAmount());
        assertEquals(0L, overview.getPendingTicketCount());
        assertNotNull(overview.getLowStockList());
        assertTrue(overview.getLowStockList().isEmpty());
    }
}
