package com.dop.web.controller.agent;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dop.common.utils.SecurityUtils;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.HashMap;
import org.springframework.web.util.UriComponentsBuilder;

/** 仅负责 Java 与 Python Agent 的内部 HTTP 通信，不承载业务工具逻辑。 */
@Service
public class AgentGatewayService
{
    private final RestClient restClient;
    private final String internalKey;
    private final RestClient businessClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonNode> tools = new HashMap<>();

    public AgentGatewayService(
            @Value("${agent.base-url:http://agent:5000}") String baseUrl,
            @Value("${agent.internal-key:}") String internalKey)
    {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.internalKey = internalKey;
        this.businessClient = RestClient.builder().baseUrl("http://localhost:8080").build();
        loadCatalog();
    }

    public Map<String, Object> health()
    {
        ResponseEntity<Map> response = call(() -> restClient.get()
                .uri("/internal/agent/health")
                .header("X-Agent-Key", internalKey)
                .retrieve()
                .toEntity(Map.class));
        return response.getBody();
    }

    public Map<String, Object> createTask(Map<String, Object> request, HttpServletRequest servletRequest)
    {
        Map<String, Object> payload = withUserContext(request, servletRequest);
        return call(() -> restClient.post().uri("/api_planning")
                .header("X-Agent-Key", internalKey)
                .body(payload).retrieve().body(Map.class));
    }

    public Map<String, Object> getTask(String taskId)
    {
        return call(() -> restClient.post().uri("/api_task_status")
                .header("X-Agent-Key", internalKey)
                .body(Map.of("task_id", taskId)).retrieve().body(Map.class));
    }

    public SseEmitter streamTask(String taskId)
    {
        SseEmitter emitter = new SseEmitter(120_000L);
        new Thread(() -> {
            try
            {
                String last = null;
                for (int i = 0; i < 120; i++)
                {
                    Map<String, Object> task = getTask(taskId);
                    String payload = objectMapper.writeValueAsString(task);
                    if (!payload.equals(last))
                    {
                        emitter.send(SseEmitter.event().name("task").data(task));
                        last = payload;
                    }
                    Object data = task.get("task");
                    if (data instanceof Map && Integer.valueOf(-1).equals(((Map<?, ?>) data).get("status")))
                    {
                        emitter.complete();
                        return;
                    }
                    Thread.sleep(500L);
                }
                emitter.complete();
            }
            catch (Exception exception)
            {
                emitter.completeWithError(exception);
            }
        }, "agent-sse-" + taskId).start();
        return emitter;
    }

    public Map<String, Object> confirmTask(String taskId, Map<String, Object> request,
            HttpServletRequest servletRequest)
    {
        Map<String, Object> payload = withUserContext(request, servletRequest);
        payload.put("taskId", taskId);
        return call(() -> restClient.post().uri("/api_planning")
                .header("X-Agent-Key", internalKey)
                .body(payload).retrieve().body(Map.class));
    }

    public Map<String, Object> inputTask(String taskId, Map<String, Object> request,
            HttpServletRequest servletRequest)
    {
        Map<String, Object> payload = withUserContext(request, servletRequest);
        payload.put("taskId", taskId);
        return call(() -> restClient.post().uri("/api_planning_input")
                .header("X-Agent-Key", internalKey)
                .body(payload).retrieve().body(Map.class));
    }

    private Map<String, Object> withUserContext(Map<String, Object> request,
            HttpServletRequest servletRequest)
    {
        Map<String, Object> payload = new java.util.HashMap<>(request == null ? Map.of() : request);
        payload.putIfAbsent("contexts", java.util.List.of());
        payload.putIfAbsent("isCopilot", true);
        payload.putIfAbsent("isContext", false);
        payload.putIfAbsent("contextNumber", 0);
        String authorization = servletRequest.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer "))
        {
            payload.put("userAuthorization", authorization);
        }
        return payload;
    }

    public Map<String, Object> cancelTask(String taskId)
    {
        return call(() -> restClient.post().uri("/internal/tasks/{taskId}/cancel", taskId)
                .header("X-Agent-Key", internalKey)
                .retrieve().body(Map.class));
    }

    public Object executeTool(String toolName, Map<String, Object> request, HttpServletRequest servletRequest)
    {
        JsonNode tool = tools.get(toolName);
        if (tool == null)
        {
            throw new AgentGatewayException("未知业务工具: " + toolName, null);
        }
        String permission = tool.path("permission").asText();
        if (!SecurityUtils.hasPermi(permission))
        {
            throw new AgentGatewayException("无权执行工具: " + toolName, null);
        }
        if ("write".equals(tool.path("operation").asText())
                && (internalKey == null || internalKey.isBlank()
                || !internalKey.equals(servletRequest.getHeader("X-Agent-Key"))))
        {
            throw new AgentGatewayException("写工具必须通过Agent确认流程执行: " + toolName, null);
        }
        String path = tool.path("path").asText();
        Map<String, Object> body = new HashMap<>(request == null ? Map.of() : request);
        for (String key : extractPathKeys(path))
        {
            Object value = body.remove(key);
            if (value == null)
            {
                throw new AgentGatewayException("工具缺少路径参数: " + key, null);
            }
            path = path.replace("{" + key + "}", String.valueOf(value));
        }
        HttpMethod method = HttpMethod.valueOf(tool.path("method").asText());
        String requestUri = path;
        if (method == HttpMethod.GET && !body.isEmpty())
        {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
            body.forEach((key, value) -> builder.queryParam(key, value));
            requestUri = builder.build().toUriString();
            body.clear();
        }
        var requestSpec = businessClient.method(method).uri(requestUri);
        String authorization = servletRequest.getHeader("Authorization");
        if (authorization != null)
        {
            requestSpec.header("Authorization", authorization);
        }
        Object response = call(() -> requestSpec.body(body).retrieve().body(Object.class));
        Map<String, Object> normalized = normalizeBusinessResponse(response);
        if (Boolean.TRUE.equals(normalized.get("success")) && "write".equals(tool.path("operation").asText()))
        {
            return verifyWriteResult(tool, normalized, servletRequest);
        }
        return normalized;
    }

    public Object executeToolByRoute(Map<String, Object> request, HttpServletRequest servletRequest)
    {
        String method = String.valueOf(request.getOrDefault("method", ""));
        String path = String.valueOf(request.getOrDefault("path", ""));
        Map<String, Object> parameters = request.get("parameters") instanceof Map
                ? (Map<String, Object>) request.get("parameters") : Map.of();
        for (Map.Entry<String, JsonNode> entry : tools.entrySet())
        {
            if (method.equalsIgnoreCase(entry.getValue().path("method").asText())
                    && path.equals(entry.getValue().path("path").asText()))
            {
                return executeTool(entry.getKey(), parameters, servletRequest);
            }
        }
        throw new AgentGatewayException("未注册业务路由: " + method + " " + path, null);
    }

    private Map<String, Object> normalizeBusinessResponse(Object response)
    {
        if (!(response instanceof Map))
        {
            return Map.of("success", true, "data", response);
        }
        Map<?, ?> raw = (Map<?, ?>) response;
        Object code = raw.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 200)
        {
            Object message = raw.containsKey("msg") ? raw.get("msg") : "业务调用失败";
            return Map.of(
                    "success", false,
                    "errorCode", code,
                    "errorMessage", String.valueOf(message));
        }
        Object data = raw.containsKey("data") ? raw.get("data") : response;
        return Map.of("success", true, "data", data);
    }

    private Map<String, Object> verifyWriteResult(JsonNode tool, Map<String, Object> execution,
            HttpServletRequest servletRequest)
    {
        String resultQuery = tool.path("resultQuery").asText("");
        if (!resultQuery.startsWith("/"))
        {
            Map<String, Object> result = new HashMap<>(execution);
            result.put("verification", Map.of("status", "not_configured"));
            return result;
        }
        if (!(execution.get("data") instanceof Map))
        {
            return failure("POSTCONDITION_MISSING", "写操作返回结果缺少可校验的数据");
        }
        Map<?, ?> data = (Map<?, ?>) execution.get("data");
        String verifyPath = resultQuery;
        for (String key : extractPathKeys(resultQuery))
        {
            Object value = data.get(key);
            if (value == null)
            {
                return failure("POSTCONDITION_MISSING", "写操作返回结果缺少校验字段: " + key);
            }
            verifyPath = verifyPath.replace("{" + key + "}", String.valueOf(value));
        }
        var verifyRequest = businessClient.method(HttpMethod.GET).uri(verifyPath);
        String authorization = servletRequest.getHeader("Authorization");
        if (authorization != null)
        {
            verifyRequest.header("Authorization", authorization);
        }
        Object verificationResponse = call(() -> verifyRequest.retrieve().body(Object.class));
        Map<String, Object> verification = normalizeBusinessResponse(verificationResponse);
        if (!Boolean.TRUE.equals(verification.get("success")))
        {
            return failure("POSTCONDITION_FAILED", String.valueOf(verification.getOrDefault("errorMessage", "写操作结果校验失败")));
        }
        Map<String, Object> result = new HashMap<>(execution);
        Map<String, Object> verificationDetail = new HashMap<>();
        verificationDetail.put("status", "verified");
        verificationDetail.put("data", verification.get("data"));
        result.put("verification", verificationDetail);
        return result;
    }

    private Map<String, Object> failure(String code, String message)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errorCode", code);
        result.put("errorMessage", message);
        return result;
    }

    private void loadCatalog()
    {
        String catalogPath = System.getProperty("agent.catalog-path", "/app/data/digital-sales-tools.json");
        try
        {
            JsonNode root = objectMapper.readTree(Files.readString(Path.of(catalogPath)));
            for (JsonNode tool : root.path("tools"))
            {
                tools.put(tool.path("name").asText(), tool);
            }
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("无法加载 Agent 工具注册表: " + catalogPath, exception);
        }
    }

    private java.util.List<String> extractPathKeys(String path)
    {
        java.util.List<String> keys = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(path);
        while (matcher.find())
        {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private <T> T call(Supplier<T> request)
    {
        try
        {
            return request.get();
        }
        catch (RestClientException exception)
        {
            throw new AgentGatewayException("Python Agent 暂不可用", exception);
        }
    }

    public static final class AgentGatewayException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public AgentGatewayException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
