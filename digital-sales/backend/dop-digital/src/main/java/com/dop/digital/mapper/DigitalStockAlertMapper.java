package com.dop.digital.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.dop.digital.domain.DigitalStockAlert;

@Mapper
public interface DigitalStockAlertMapper
{
    List<DigitalStockAlert> selectLowStockSkus();
    List<Long> selectActiveSkuIds();
    int insertStockAlert(DigitalStockAlert alert);
    int resolveStockAlert(Long skuId);
}
