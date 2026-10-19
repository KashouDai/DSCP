package com.dop.web.controller.digital;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.dop.common.utils.poi.ExcelUtil;
import com.dop.digital.domain.DigitalProduct;
import com.dop.digital.service.IDigitalProductService;

/**
 * 商品
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/product")
public class DigitalProductController extends BaseController
{
    @Autowired
    private IDigitalProductService productService;

    /**
     * 查询商品列表
     */
    @PreAuthorize("@ss.hasPermi('digital:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalProduct product)
    {
        startPage();
        List<DigitalProduct> list = productService.selectDigitalProductList(product);
        return getDataTable(list);
    }

    /**
     * 导出商品列表
     */
    @Log(title = "商品管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('digital:product:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, DigitalProduct product)
    {
        List<DigitalProduct> list = productService.selectDigitalProductList(product);
        ExcelUtil<DigitalProduct> util = new ExcelUtil<DigitalProduct>(DigitalProduct.class);
        util.exportExcel(response, list, "商品数据");
    }

    /**
     * 获取商品详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:product:query')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable Long productId)
    {
        return success(productService.selectDigitalProductById(productId));
    }

    /**
     * 新增商品
     */
    @PreAuthorize("@ss.hasPermi('digital:product:add')")
    @Log(title = "商品管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DigitalProduct product)
    {
        product.setCreateBy(getUsername());
        return toAjax(productService.insertDigitalProduct(product));
    }

    /**
     * 修改商品
     */
    @PreAuthorize("@ss.hasPermi('digital:product:edit')")
    @Log(title = "商品管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DigitalProduct product)
    {
        product.setUpdateBy(getUsername());
        return toAjax(productService.updateDigitalProduct(product));
    }

    /**
     * 删除商品
     */
    @PreAuthorize("@ss.hasPermi('digital:product:remove')")
    @Log(title = "商品管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(productService.deleteDigitalProductByIds(productIds));
    }
}
