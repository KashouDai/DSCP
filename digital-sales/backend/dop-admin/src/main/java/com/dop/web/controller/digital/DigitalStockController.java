package com.dop.web.controller.digital;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dop.common.annotation.Log;
import com.dop.common.annotation.RepeatSubmit;
import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import com.dop.common.core.page.TableDataInfo;
import com.dop.common.enums.BusinessType;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.domain.DigitalStockInboundRequest;
import com.dop.digital.domain.DigitalStockRecord;
import com.dop.digital.service.IDigitalStockService;

/**
 * 库存
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/stock")
public class DigitalStockController extends BaseController
{
    @Autowired
    private IDigitalStockService stockService;

    /**
     * 查询当前库存列表
     */
    @PreAuthorize("@ss.hasPermi('digital:stock:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalProductSku sku)
    {
        startPage();
        List<DigitalProductSku> list = stockService.selectStockList(sku);
        return getDataTable(list);
    }

    /**
     * 查询库存流水列表
     */
    @PreAuthorize("@ss.hasPermi('digital:stock:record')")
    @GetMapping("/record/list")
    public TableDataInfo recordList(DigitalStockRecord record)
    {
        startPage();
        List<DigitalStockRecord> list = stockService.selectDigitalStockRecordList(record);
        return getDataTable(list);
    }

    /**
     * 手工入库
     */
    @PreAuthorize("@ss.hasPermi('digital:stock:in')")
    @Log(title = "库存管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/inbound")
    public AjaxResult inbound(@Validated @RequestBody DigitalStockInboundRequest request)
    {
        return success(stockService.inbound(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('digital:stock:in')")
    @Log(title = "库存管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/outbound")
    public AjaxResult outbound(@Validated @RequestBody DigitalStockInboundRequest request)
    {
        return success(stockService.outbound(request, getUsername()));
    }
}
