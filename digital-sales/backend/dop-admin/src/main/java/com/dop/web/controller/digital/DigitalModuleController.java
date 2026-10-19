package com.dop.web.controller.digital;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 数码业务模块基础接口
 *
 * @author dop
 */
@Tag(name = "数码业务模块")
@RestController
@RequestMapping("/digital/product")
public class DigitalModuleController extends BaseController
{
    /**
     * 验证业务模块和接口权限已接入
     */
    @Operation(summary = "检查业务模块")
    @PreAuthorize("@ss.hasPermi('digital:product:list')")
    @GetMapping("/health")
    public AjaxResult health()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", "dop-digital");
        data.put("operator", getUsername());
        return success(data);
    }
}
