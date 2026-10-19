package com.dop.web.controller.digital;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import com.dop.digital.agent.query.AgentQueryException;
import com.dop.digital.agent.query.domain.AgentQueryPlan;
import com.dop.digital.service.IAgentQueryService;

/** Agent 只读查询入口。 */
@RestController
@RequestMapping("/digital/agent")
public class AgentQueryController extends BaseController
{
    @Autowired
    private IAgentQueryService agentQueryService;

    @PreAuthorize("@ss.hasPermi('digital:dashboard:list')")
    @PostMapping("/query")
    public AjaxResult query(@RequestBody AgentQueryPlan plan)
    {
        try
        {
            return success(agentQueryService.execute(plan));
        }
        catch (AgentQueryException exception)
        {
            return error(exception.getMessage());
        }
    }
}
