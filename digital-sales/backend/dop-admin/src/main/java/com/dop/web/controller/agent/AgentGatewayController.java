package com.dop.web.controller.agent;

import com.dop.common.core.controller.BaseController;
import com.dop.common.core.domain.AjaxResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

/** digital-sales 到 Python Agent 的内部网关。 */
@RestController
@RequestMapping("/agent")
public class AgentGatewayController extends BaseController
{
    private final AgentGatewayService agentGatewayService;

    public AgentGatewayController(AgentGatewayService agentGatewayService)
    {
        this.agentGatewayService = agentGatewayService;
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @GetMapping("/health")
    public AjaxResult health()
    {
        return success(agentGatewayService.health());
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @org.springframework.web.bind.annotation.PostMapping("/tasks")
    public AjaxResult createTask(@RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpServletRequest servletRequest)
    {
        return success(agentGatewayService.createTask(request, servletRequest));
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @GetMapping("/tasks/{taskId}")
    public AjaxResult getTask(@PathVariable String taskId)
    {
        return success(agentGatewayService.getTask(taskId));
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @GetMapping("/tasks/{taskId}/events")
    public SseEmitter streamTask(@PathVariable String taskId)
    {
        return agentGatewayService.streamTask(taskId);
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @org.springframework.web.bind.annotation.PostMapping("/tasks/{taskId}/confirm")
    public AjaxResult confirmTask(@PathVariable String taskId, @RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpServletRequest servletRequest)
    {
        return success(agentGatewayService.confirmTask(taskId, request, servletRequest));
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @org.springframework.web.bind.annotation.PostMapping("/tasks/{taskId}/input")
    public AjaxResult inputTask(@PathVariable String taskId, @RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpServletRequest servletRequest)
    {
        return success(agentGatewayService.inputTask(taskId, request, servletRequest));
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @org.springframework.web.bind.annotation.PostMapping("/tasks/{taskId}/cancel")
    public AjaxResult cancelTask(@PathVariable String taskId)
    {
        return success(agentGatewayService.cancelTask(taskId));
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @org.springframework.web.bind.annotation.PostMapping("/tools/{toolName}")
    public AjaxResult executeTool(@PathVariable String toolName, @RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpServletRequest servletRequest)
    {
        return success(agentGatewayService.executeTool(toolName, request, servletRequest));
    }

    @PreAuthorize("@ss.hasPermi('digital:agent:use')")
    @org.springframework.web.bind.annotation.PostMapping("/tools/execute")
    public AjaxResult executeToolByRoute(@RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpServletRequest servletRequest)
    {
        return success(agentGatewayService.executeToolByRoute(request, servletRequest));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AgentGatewayService.AgentGatewayException.class)
    public ResponseEntity<AjaxResult> handleAgentUnavailable(AgentGatewayService.AgentGatewayException exception)
    {
        return ResponseEntity.status(503).body(AjaxResult.error(503, exception.getMessage()));
    }
}
