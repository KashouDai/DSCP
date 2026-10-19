package com.dop.digital.service;

import java.util.List;
import com.dop.digital.domain.DigitalProductCategory;

/**
 * 商品分类 服务层
 *
 * @author dop
 */
public interface IDigitalProductCategoryService
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
     * @param category 商品分类
     * @return 结果
     */
    public boolean checkCategoryNameUnique(DigitalProductCategory category);

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
