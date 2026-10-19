package com.dop.digital.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dop.digital.domain.DigitalStockAlert;
import com.dop.digital.mapper.DigitalStockAlertMapper;
import com.dop.digital.service.IDigitalStockAlertService;

@Service("digitalLowStockAlertService")
public class DigitalStockAlertServiceImpl implements IDigitalStockAlertService
{
    private final DigitalStockAlertMapper alertMapper;

    public DigitalStockAlertServiceImpl(DigitalStockAlertMapper alertMapper)
    {
        this.alertMapper = alertMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scan()
    {
        List<DigitalStockAlert> lowStockSkus = alertMapper.selectLowStockSkus();
        Set<Long> lowStockIds = new HashSet<>();
        Set<Long> activeIds = new HashSet<>(alertMapper.selectActiveSkuIds());
        for (DigitalStockAlert sku : lowStockSkus)
        {
            lowStockIds.add(sku.getSkuId());
            if (!activeIds.contains(sku.getSkuId()))
            {
                alertMapper.insertStockAlert(sku);
            }
        }
        for (Long skuId : activeIds)
        {
            if (!lowStockIds.contains(skuId))
            {
                alertMapper.resolveStockAlert(skuId);
            }
        }
    }
}
