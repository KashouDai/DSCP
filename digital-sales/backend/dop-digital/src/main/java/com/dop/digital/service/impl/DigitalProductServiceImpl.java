package com.dop.digital.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalProduct;
import com.dop.digital.mapper.DigitalProductCategoryMapper;
import com.dop.digital.mapper.DigitalProductMapper;
import com.dop.digital.mapper.DigitalProductSkuMapper;
import com.dop.digital.service.IDigitalProductService;

/**
 * 商品 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalProductServiceImpl implements IDigitalProductService
{
    @Autowired
    private DigitalProductMapper productMapper;

    @Autowired
    private DigitalProductCategoryMapper categoryMapper;

    @Autowired
    private DigitalProductSkuMapper skuMapper;

    @Override
    public List<DigitalProduct> selectDigitalProductList(DigitalProduct product)
    {
        return productMapper.selectDigitalProductList(product);
    }

    @Override
    public DigitalProduct selectDigitalProductById(Long productId)
    {
        return productMapper.selectDigitalProductById(productId);
    }

    @Override
    public int insertDigitalProduct(DigitalProduct product)
    {
        checkCategory(product.getCategoryId());
        return productMapper.insertDigitalProduct(product);
    }

    @Override
    public int updateDigitalProduct(DigitalProduct product)
    {
        if (StringUtils.isNotNull(product.getCategoryId()))
        {
            checkCategory(product.getCategoryId());
        }
        return productMapper.updateDigitalProduct(product);
    }

    @Override
    public int deleteDigitalProductByIds(Long[] productIds)
    {
        for (Long productId : productIds)
        {
            DigitalProduct product = productMapper.selectDigitalProductById(productId);
            if (StringUtils.isNull(product))
            {
                continue;
            }
            if (skuMapper.countSkuByProductId(productId) > 0)
            {
                throw new ServiceException(String.format("%1$s已关联规格，不能删除", product.getProductName()));
            }
        }
        return productMapper.deleteDigitalProductByIds(productIds);
    }

    private void checkCategory(Long categoryId)
    {
        if (StringUtils.isNull(categoryMapper.selectDigitalProductCategoryById(categoryId)))
        {
            throw new ServiceException("商品分类不存在");
        }
    }
}
