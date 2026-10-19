package com.dop.web.controller.digital;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.HashMap;
import java.util.Map;
import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import com.dop.digital.service.IDigitalDashboardService;

/**
 * 数码业务首页看板
 *
 * @author dop
 */
@RestController
@RequestMapping("/digital/dashboard")
public class DigitalDashboardController extends BaseController
{
    @Autowired
    private IDigitalDashboardService dashboardService;

    /**
     * 查询首页看板概览
     */
    @PreAuthorize("@ss.hasPermi('digital:dashboard:list')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return success(dashboardService.selectDashboardOverview());
    }

    @PreAuthorize("@ss.hasPermi('digital:dashboard:list')")
    @PostMapping("/analytics")
    public AjaxResult analytics(@RequestBody Map<String, Object> request)
    {
        String metric = String.valueOf(request.get("metric"));
        Map<String, Object> filters = request.get("filters") instanceof Map
                ? new HashMap<>((Map<String, Object>) request.get("filters")) : new HashMap<>();
        return success(dashboardService.queryAnalytics(metric, filters));
    }
}
