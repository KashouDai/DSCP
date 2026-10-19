package com.dop.web.controller.digital;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dop.common.annotation.Log;
import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import com.dop.common.core.page.TableDataInfo;
import com.dop.common.enums.BusinessType;
import com.dop.digital.domain.DigitalProductCategory;
import com.dop.digital.service.IDigitalProductCategoryService;

/**
 * 商品分类
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/category")
public class DigitalProductCategoryController extends BaseController
{
    @Autowired
    private IDigitalProductCategoryService categoryService;

    /**
     * 查询商品分类列表
     */
    @PreAuthorize("@ss.hasPermi('digital:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalProductCategory category)
    {
        startPage();
        List<DigitalProductCategory> list = categoryService.selectDigitalProductCategoryList(category);
        return getDataTable(list);
    }

    /**
     * 获取商品分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:category:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId)
    {
        return success(categoryService.selectDigitalProductCategoryById(categoryId));
    }

    /**
     * 新增商品分类
     */
    @PreAuthorize("@ss.hasPermi('digital:category:add')")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DigitalProductCategory category)
    {
        if (!categoryService.checkCategoryNameUnique(category))
        {
            return error("新增商品分类'" + category.getCategoryName() + "'失败，分类名称已存在");
        }
        category.setCreateBy(getUsername());
        return toAjax(categoryService.insertDigitalProductCategory(category));
    }

    /**
     * 修改商品分类
     */
    @PreAuthorize("@ss.hasPermi('digital:category:edit')")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DigitalProductCategory category)
    {
        if (!categoryService.checkCategoryNameUnique(category))
        {
            return error("修改商品分类'" + category.getCategoryName() + "'失败，分类名称已存在");
        }
        category.setUpdateBy(getUsername());
        return toAjax(categoryService.updateDigitalProductCategory(category));
    }

    /**
     * 删除商品分类
     */
    @PreAuthorize("@ss.hasPermi('digital:category:remove')")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(categoryService.deleteDigitalProductCategoryByIds(categoryIds));
    }
}
