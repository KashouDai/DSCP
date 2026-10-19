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
import com.dop.digital.domain.DigitalCustomer;
import com.dop.digital.service.IDigitalCustomerService;

/**
 * 客户
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/customer")
public class DigitalCustomerController extends BaseController
{
    @Autowired
    private IDigitalCustomerService customerService;

    /**
     * 查询客户列表
     */
    @PreAuthorize("@ss.hasPermi('digital:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalCustomer customer)
    {
        startPage();
        List<DigitalCustomer> list = customerService.selectDigitalCustomerList(customer);
        return getDataTable(list);
    }

    /**
     * 导出客户列表
     */
    @Log(title = "客户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('digital:customer:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, DigitalCustomer customer)
    {
        List<DigitalCustomer> list = customerService.selectDigitalCustomerList(customer);
        ExcelUtil<DigitalCustomer> util = new ExcelUtil<DigitalCustomer>(DigitalCustomer.class);
        util.exportExcel(response, list, "客户数据");
    }

    /**
     * 获取客户详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:customer:query')")
    @GetMapping(value = "/{customerId}")
    public AjaxResult getInfo(@PathVariable Long customerId)
    {
        return success(customerService.selectDigitalCustomerById(customerId));
    }

    /**
     * 新增客户
     */
    @PreAuthorize("@ss.hasPermi('digital:customer:add')")
    @Log(title = "客户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DigitalCustomer customer)
    {
        customer.setCreateBy(getUsername());
        return toAjax(customerService.insertDigitalCustomer(customer));
    }

    /**
     * 修改客户
     */
    @PreAuthorize("@ss.hasPermi('digital:customer:edit')")
    @Log(title = "客户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DigitalCustomer customer)
    {
        customer.setUpdateBy(getUsername());
        return toAjax(customerService.updateDigitalCustomer(customer));
    }

    /**
     * 删除客户
     */
    @PreAuthorize("@ss.hasPermi('digital:customer:remove')")
    @Log(title = "客户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{customerIds}")
    public AjaxResult remove(@PathVariable Long[] customerIds)
    {
        return toAjax(customerService.deleteDigitalCustomerByIds(customerIds));
    }
}
