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
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.service.IDigitalProductSkuService;

/**
 * 商品规格
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/sku")
public class DigitalProductSkuController extends BaseController
{
    @Autowired
    private IDigitalProductSkuService skuService;

    /**
     * 查询商品规格列表
     */
    @PreAuthorize("@ss.hasPermi('digital:sku:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalProductSku sku)
    {
        startPage();
        List<DigitalProductSku> list = skuService.selectDigitalProductSkuList(sku);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('digital:sku:add')")
    @GetMapping("/generate-barcode")
    public AjaxResult generateBarcode()
    {
        return AjaxResult.success("操作成功", skuService.generateBarcode());
    }

    /**
     * 获取商品规格详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:sku:query')")
    @GetMapping(value = "/{skuId}")
    public AjaxResult getInfo(@PathVariable Long skuId)
    {
        return success(skuService.selectDigitalProductSkuById(skuId));
    }

    /**
     * 新增商品规格
     */
    @PreAuthorize("@ss.hasPermi('digital:sku:add')")
    @Log(title = "商品规格", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DigitalProductSku sku)
    {
        if (!skuService.checkSkuCodeUnique(sku))
        {
            return error("新增商品规格'" + sku.getSkuName() + "'失败，规格编码已存在");
        }
        if (!skuService.checkBarcodeUnique(sku))
        {
            return error("新增商品规格失败，条码已存在");
        }
        sku.setCreateBy(getUsername());
        return toAjax(skuService.insertDigitalProductSku(sku));
    }

    /**
     * 修改商品规格
     */
    @PreAuthorize("@ss.hasPermi('digital:sku:edit')")
    @Log(title = "商品规格", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DigitalProductSku sku)
    {
        if (!skuService.checkSkuCodeUnique(sku))
        {
            return error("修改商品规格'" + sku.getSkuName() + "'失败，规格编码已存在");
        }
        if (!skuService.checkBarcodeUnique(sku))
        {
            return error("修改商品规格失败，条码已存在");
        }
        sku.setUpdateBy(getUsername());
        return toAjax(skuService.updateDigitalProductSku(sku));
    }

    /**
     * 删除商品规格
     */
    @PreAuthorize("@ss.hasPermi('digital:sku:remove')")
    @Log(title = "商品规格", businessType = BusinessType.DELETE)
    @DeleteMapping("/{skuIds}")
    public AjaxResult remove(@PathVariable Long[] skuIds)
    {
        return toAjax(skuService.deleteDigitalProductSkuByIds(skuIds));
    }
}
