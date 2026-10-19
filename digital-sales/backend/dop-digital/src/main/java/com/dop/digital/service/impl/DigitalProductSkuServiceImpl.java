package com.dop.digital.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dop.common.constant.UserConstants;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.mapper.DigitalProductMapper;
import com.dop.digital.mapper.DigitalProductSkuMapper;
import com.dop.digital.mapper.DigitalSaleOrderItemMapper;
import com.dop.digital.service.IDigitalProductSkuService;

/**
 * 商品规格 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalProductSkuServiceImpl implements IDigitalProductSkuService
{
    @Autowired
    private DigitalProductSkuMapper skuMapper;

    @Autowired
    private DigitalProductMapper productMapper;

    @Autowired
    private DigitalSaleOrderItemMapper orderItemMapper;

    @Override
    public List<DigitalProductSku> selectDigitalProductSkuList(DigitalProductSku sku)
    {
        return skuMapper.selectDigitalProductSkuList(sku);
    }

    @Override
    public boolean checkBarcodeUnique(DigitalProductSku sku)
    {
        return StringUtils.isEmpty(sku.getBarcode())
                || StringUtils.isNull(skuMapper.checkBarcodeUnique(sku.getBarcode(), sku.getSkuId()));
    }

    @Override
    public String generateBarcode()
    {
        for (int i = 0; i < 20; i++)
        {
            String base = "200" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
            int sum = 0;
            for (int j = 0; j < base.length(); j++)
            {
                int digit = base.charAt(j) - '0';
                sum += (j % 2 == 0) ? digit : digit * 3;
            }
            String barcode = base + ((10 - sum % 10) % 10);
            if (StringUtils.isNull(skuMapper.checkBarcodeUnique(barcode, null)))
            {
                return barcode;
            }
        }
        throw new ServiceException("条码生成失败，请稍后重试");
    }

    @Override
    public DigitalProductSku selectDigitalProductSkuById(Long skuId)
    {
        return skuMapper.selectDigitalProductSkuById(skuId);
    }

    @Override
    public boolean checkSkuCodeUnique(DigitalProductSku sku)
    {
        Long skuId = StringUtils.isNull(sku.getSkuId()) ? -1L : sku.getSkuId();
        DigitalProductSku info = skuMapper.checkSkuCodeUnique(sku.getSkuCode());
        if (StringUtils.isNotNull(info) && info.getSkuId().longValue() != skuId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertDigitalProductSku(DigitalProductSku sku)
    {
        checkProduct(sku.getProductId());
        if (StringUtils.isEmpty(sku.getBarcode()))
        {
            sku.setBarcode(null);
        }
        if (StringUtils.isNull(sku.getCostPrice()))
        {
            sku.setCostPrice(BigDecimal.ZERO);
        }
        return skuMapper.insertDigitalProductSku(sku);
    }

    @Override
    public int updateDigitalProductSku(DigitalProductSku sku)
    {
        if (StringUtils.isEmpty(sku.getBarcode()))
        {
            sku.setBarcode(null);
        }
        if (StringUtils.isNotNull(sku.getProductId()))
        {
            checkProduct(sku.getProductId());
        }
        return skuMapper.updateDigitalProductSku(sku);
    }

    @Override
    public int deleteDigitalProductSkuByIds(Long[] skuIds)
    {
        for (Long skuId : skuIds)
        {
            DigitalProductSku sku = skuMapper.selectDigitalProductSkuById(skuId);
            if (StringUtils.isNotNull(sku) && orderItemMapper.countOrderItemBySkuId(skuId) > 0)
            {
                throw new ServiceException(String.format("%1$s已被订单引用，只能下架，不能删除", sku.getSkuName()));
            }
        }
        return skuMapper.deleteDigitalProductSkuByIds(skuIds);
    }

    private void checkProduct(Long productId)
    {
        if (StringUtils.isNull(productMapper.selectDigitalProductById(productId)))
        {
            throw new ServiceException("商品不存在");
        }
    }
}
