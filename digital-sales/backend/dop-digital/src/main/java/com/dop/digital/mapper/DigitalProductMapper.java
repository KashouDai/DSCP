package com.dop.digital.mapper;

import java.util.List;
import com.dop.digital.domain.DigitalProduct;

/**
 * 商品 数据层
 *
 * @author dop
 */
public interface DigitalProductMapper
{
    /**
     * 查询商品列表
     *
     * @param product 商品
     * @return 商品集合
     */
    public List<DigitalProduct> selectDigitalProductList(DigitalProduct product);

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品
     */
    public DigitalProduct selectDigitalProductById(Long productId);

    /**
     * 新增商品
     *
     * @param product 商品
     * @return 结果
     */
    public int insertDigitalProduct(DigitalProduct product);

    /**
     * 修改商品
     *
     * @param product 商品
     * @return 结果
     */
    public int updateDigitalProduct(DigitalProduct product);

    /**
     * 逻辑删除商品
     *
     * @param productIds 商品ID数组
     * @return 结果
     */
    public int deleteDigitalProductByIds(Long[] productIds);
}
