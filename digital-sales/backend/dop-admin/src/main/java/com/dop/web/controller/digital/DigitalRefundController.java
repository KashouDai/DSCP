package com.dop.web.controller.digital;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.dop.digital.domain.DigitalRefundRecord;
import com.dop.digital.service.IDigitalRefundService;

/**
 * 退款记录
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/refund")
public class DigitalRefundController extends BaseController
{
    @Autowired
    private IDigitalRefundService refundService;

    /**
     * 查询退款记录列表
     */
    @PreAuthorize("@ss.hasPermi('digital:refund:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalRefundRecord refund)
    {
        startPage();
        List<DigitalRefundRecord> list = refundService.selectDigitalRefundRecordList(refund);
        return getDataTable(list);
    }

    /**
     * 获取退款记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:refund:query')")
    @GetMapping("/{refundId}")
    public AjaxResult getInfo(@PathVariable Long refundId)
    {
        return success(refundService.selectDigitalRefundRecordById(refundId));
    }

    /**
     * 审核退款
     */
    @PreAuthorize("@ss.hasPermi('digital:refund:audit')")
    @Log(title = "退款管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{refundId}/audit")
    public AjaxResult audit(@PathVariable Long refundId,
            @RequestBody DigitalRefundRecord refund)
    {
        return success(refundService.auditDigitalRefund(refundId, refund,
                getUserId(), getUsername()));
    }

    /**
     * 取消退款
     */
    @PreAuthorize("@ss.hasPermi('digital:refund:edit')")
    @Log(title = "退款管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{refundId}/cancel")
    public AjaxResult cancel(@PathVariable Long refundId,
            @RequestBody DigitalRefundRecord refund)
    {
        return success(refundService.cancelDigitalRefund(refundId, refund,
                getUserId(), getUsername()));
    }
}
