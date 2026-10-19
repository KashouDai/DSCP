package com.dop.digital.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dop.common.constant.UserConstants;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalProductCategory;
import com.dop.digital.mapper.DigitalProductCategoryMapper;
import com.dop.digital.service.IDigitalProductCategoryService;

/**
 * 商品分类 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalProductCategoryServiceImpl implements IDigitalProductCategoryService
{
    @Autowired
    private DigitalProductCategoryMapper categoryMapper;

    @Override
    public List<DigitalProductCategory> selectDigitalProductCategoryList(DigitalProductCategory category)
    {
        return categoryMapper.selectDigitalProductCategoryList(category);
    }

    @Override
    public DigitalProductCategory selectDigitalProductCategoryById(Long categoryId)
    {
        return categoryMapper.selectDigitalProductCategoryById(categoryId);
    }

    @Override
    public boolean checkCategoryNameUnique(DigitalProductCategory category)
    {
        Long categoryId = StringUtils.isNull(category.getCategoryId()) ? -1L : category.getCategoryId();
        DigitalProductCategory info = categoryMapper.checkCategoryNameUnique(category.getCategoryName());
        if (StringUtils.isNotNull(info) && info.getCategoryId().longValue() != categoryId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertDigitalProductCategory(DigitalProductCategory category)
    {
        return categoryMapper.insertDigitalProductCategory(category);
    }

    @Override
    public int updateDigitalProductCategory(DigitalProductCategory category)
    {
        return categoryMapper.updateDigitalProductCategory(category);
    }

    @Override
    public int deleteDigitalProductCategoryByIds(Long[] categoryIds)
    {
        for (Long categoryId : categoryIds)
        {
            DigitalProductCategory category = categoryMapper.selectDigitalProductCategoryById(categoryId);
            if (StringUtils.isNull(category))
            {
                continue;
            }
            if (categoryMapper.countProductByCategoryId(categoryId) > 0)
            {
                throw new ServiceException(String.format("%1$s已关联商品，不能删除", category.getCategoryName()));
            }
        }
        return categoryMapper.deleteDigitalProductCategoryByIds(categoryIds);
    }
}
