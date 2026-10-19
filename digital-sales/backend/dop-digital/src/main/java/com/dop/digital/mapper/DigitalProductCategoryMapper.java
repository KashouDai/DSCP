package com.dop.digital.mapper;

import java.util.List;
import com.dop.digital.domain.DigitalProductCategory;

/**
 * 商品分类 数据层
 *
 * @author dop
 */
public interface DigitalProductCategoryMapper
{
    /**
     * 查询商品分类列表
     *
     * @param category 商品分类
     * @return 商品分类集合
     */
    public List<DigitalProductCategory> selectDigitalProductCategoryList(DigitalProductCategory category);

    /**
     * 根据分类ID查询商品分类
     *
     * @param categoryId 分类ID
     * @return 商品分类
     */
    public DigitalProductCategory selectDigitalProductCategoryById(Long categoryId);

    /**
     * 校验分类名称是否唯一
     *
     * @param categoryName 分类名称
     * @return 商品分类
     */
    public DigitalProductCategory checkCategoryNameUnique(String categoryName);

    /**
     * 统计分类下的商品数量
     *
     * @param categoryId 分类ID
     * @return 商品数量
     */
    public int countProductByCategoryId(Long categoryId);

    /**
     * 新增商品分类
     *
     * @param category 商品分类
     * @return 结果
     */
    public int insertDigitalProductCategory(DigitalProductCategory category);

    /**
     * 修改商品分类
     *
     * @param category 商品分类
     * @return 结果
     */
    public int updateDigitalProductCategory(DigitalProductCategory category);

    /**
     * 逻辑删除商品分类
     *
     * @param categoryIds 分类ID数组
     * @return 结果
     */
    public int deleteDigitalProductCategoryByIds(Long[] categoryIds);
}
