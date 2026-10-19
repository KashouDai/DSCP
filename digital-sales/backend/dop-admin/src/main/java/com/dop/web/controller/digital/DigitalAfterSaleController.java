package com.dop.web.controller.digital;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.dop.common.core.domain.entity.SysUser;
import com.dop.common.core.page.TableDataInfo;
import com.dop.common.enums.BusinessType;
import com.dop.digital.domain.DigitalAfterSaleTicket;
import com.dop.digital.service.IDigitalAfterSaleService;

/**
 * 售后工单
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/aftersale")
public class DigitalAfterSaleController extends BaseController
{
    @Autowired
    private IDigitalAfterSaleService afterSaleService;

    /**
     * 查询售后工单列表
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:list')")
    @GetMapping("/list")
    public TableDataInfo list(DigitalAfterSaleTicket ticket)
    {
        startPage();
        List<DigitalAfterSaleTicket> list = afterSaleService.selectDigitalAfterSaleTicketList(ticket);
        return getDataTable(list);
    }

    /**
     * 获取售后工单详细信息
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:query')")
    @GetMapping("/{ticketId}")
    public AjaxResult getInfo(@PathVariable Long ticketId)
    {
        return success(afterSaleService.selectDigitalAfterSaleTicketById(ticketId));
    }

    /**
     * 从已完成订单明细创建售后工单
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:add')")
    @Log(title = "售后管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DigitalAfterSaleTicket ticket)
    {
        return success(afterSaleService.createDigitalAfterSaleTicket(ticket, getUsername()));
    }

    /**
     * 分配处理人
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:assign')")
    @Log(title = "售后管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{ticketId}/assign")
    public AjaxResult assign(@PathVariable Long ticketId, @RequestBody DigitalAfterSaleTicket ticket)
    {
        return success(afterSaleService.assignDigitalAfterSaleTicket(ticketId,
                ticket.getHandlerId(), getUsername()));
    }

    /**
     * 记录处理进展
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:handle')")
    @Log(title = "售后管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{ticketId}/handle")
    public AjaxResult handle(@PathVariable Long ticketId, @RequestBody DigitalAfterSaleTicket ticket)
    {
        ticket.setTicketId(ticketId);
        return success(afterSaleService.handleDigitalAfterSaleTicket(ticket, getUsername()));
    }

    /**
     * 完成维修工单
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:complete')")
    @Log(title = "售后管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{ticketId}/complete")
    public AjaxResult complete(@PathVariable Long ticketId, @RequestBody DigitalAfterSaleTicket ticket)
    {
        return success(afterSaleService.completeDigitalAfterSaleTicket(ticketId,
                ticket.getHandleResult(), getUsername()));
    }

    /**
     * 退货检查并登记实收数量和处理结论
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:complete')")
    @Log(title = "售后管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{ticketId}/return-check")
    public AjaxResult returnCheck(@PathVariable Long ticketId,
            @RequestBody DigitalAfterSaleTicket ticket)
    {
        return success(afterSaleService.checkReturnDigitalAfterSaleTicket(ticketId, ticket,
                getUsername()));
    }

    /**
     * 关闭售后工单
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:close')")
    @Log(title = "售后管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{ticketId}/close")
    public AjaxResult close(@PathVariable Long ticketId, @RequestBody DigitalAfterSaleTicket ticket)
    {
        return success(afterSaleService.closeDigitalAfterSaleTicket(ticketId,
                ticket.getHandleResult(), getUsername()));
    }

    /**
     * 查询可选处理人
     */
    @PreAuthorize("@ss.hasPermi('digital:aftersale:assign')")
    @GetMapping("/handlers")
    public AjaxResult handlers()
    {
        List<Map<String, Object>> handlers = new ArrayList<>();
        for (SysUser user : afterSaleService.selectHandlerList())
        {
            Map<String, Object> handler = new LinkedHashMap<>();
            handler.put("userId", user.getUserId());
            handler.put("userName", user.getUserName());
            handler.put("nickName", user.getNickName());
            handlers.add(handler);
        }
        return success(handlers);
    }
}
