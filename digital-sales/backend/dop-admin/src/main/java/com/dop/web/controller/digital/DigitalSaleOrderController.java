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
import com.dop.common.annotation.RepeatSubmit;
import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import com.dop.common.core.page.TableDataInfo;
import com.dop.common.enums.BusinessType;
import com.dop.common.utils.poi.ExcelUtil;
import com.dop.digital.domain.DigitalSaleOrder;
import com.dop.digital.service.IDigitalSaleOrderService;

/**
 * 销售订单
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/order")
public class DigitalSaleOrderController extends BaseController
{
    @Autowired
    private IDigitalSaleOrderService saleOrderService;

    /**
     * 查询销售订单列表
     */
    @PreAuthorize("@ss.hasPermi('digital:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalSaleOrder order)
    {
        startPage();
        List<DigitalSaleOrder> list = saleOrderService.selectDigitalSaleOrderList(order);
        return getDataTable(list);
    }

    /**
     * 导出销售订单列表
     */
    @Log(title = "订单管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('digital:order:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, DigitalSaleOrder order)
    {
        List<DigitalSaleOrder> list = saleOrderService.selectDigitalSaleOrderList(order);
        ExcelUtil<DigitalSaleOrder> util = new ExcelUtil<DigitalSaleOrder>(DigitalSaleOrder.class);
        util.exportExcel(response, list, "订单数据");
    }

    /**
     * 获取销售订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:order:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId)
    {
        return success(saleOrderService.selectDigitalSaleOrderById(orderId));
    }

    /**
     * 新增草稿订单
     */
    @PreAuthorize("@ss.hasPermi('digital:order:add')")
    @Log(title = "订单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DigitalSaleOrder order)
    {
        return success(saleOrderService.createDigitalSaleOrderDraft(order, getUsername()));
    }

    /**
     * 修改草稿订单
     */
    @PreAuthorize("@ss.hasPermi('digital:order:edit')")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DigitalSaleOrder order)
    {
        return success(saleOrderService.updateDigitalSaleOrderDraft(order, getUsername()));
    }

    /**
     * 确认订单
     */
    @PreAuthorize("@ss.hasPermi('digital:order:confirm')")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{orderId}/confirm")
    public AjaxResult confirm(@PathVariable Long orderId)
    {
        return success(saleOrderService.confirmDigitalSaleOrder(orderId, getUsername()));
    }

    /**
     * 取消订单
     */
    @PreAuthorize("@ss.hasPermi('digital:order:cancel')")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{orderId}/cancel")
    public AjaxResult cancel(@PathVariable Long orderId)
    {
        return success(saleOrderService.cancelDigitalSaleOrder(orderId, getUsername()));
    }

    /**
     * 完成订单
     */
    @PreAuthorize("@ss.hasPermi('digital:order:complete')")
    @Log(title = "订单管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{orderId}/complete")
    public AjaxResult complete(@PathVariable Long orderId)
    {
        return success(saleOrderService.completeDigitalSaleOrder(orderId, getUsername()));
    }

    /**
     * 删除草稿订单
     */
    @PreAuthorize("@ss.hasPermi('digital:order:remove')")
    @Log(title = "订单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(saleOrderService.deleteDigitalSaleOrderByIds(orderIds));
    }
}
