package org.aileme.system.controller.system;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.domain.R;
import org.aileme.common.core.utils.StringUtils;
import org.aileme.common.web.core.BaseController;
import org.aileme.system.domain.SysThirdPartyConfigItem;
import org.aileme.system.domain.SysThirdPartyProvider;
import org.aileme.system.domain.SysThirdPartyRouteRule;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方服务配置Controller
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/system/thirdparty")
@RequiredArgsConstructor
public class SysThirdPartyController extends BaseController {

    private final ISysThirdPartyService thirdPartyService;
    private final Environment environment;
    private final ApplicationContext applicationContext;

    private volatile RestTemplate runtimeAdminRestTemplate;

    /**
     * 获取所有服务类型
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/serviceTypes")
    public R<List<String>> listServiceTypes() {
        return R.ok(thirdPartyService.listServiceTypes());
    }

    /**
     * 获取指定服务的所有提供商
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/providers/{serviceType}")
    public R<List<SysThirdPartyProvider>> listProviders(@PathVariable String serviceType) {
        return R.ok(thirdPartyService.listProviders(serviceType));
    }

    /**
     * 获取当前使用的提供商
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/providers/{serviceType}/current")
    public R<SysThirdPartyProvider> getCurrentProvider(@PathVariable String serviceType) {
        return R.ok(thirdPartyService.getCurrentProvider(serviceType));
    }

    /**
     * 切换服务提供商
     */
    @SaCheckPermission("system:thirdparty:switch")
    @PutMapping("/providers/{serviceType}/switch")
    public R<Void> switchProvider(
            @PathVariable String serviceType,
            @RequestBody Map<String, String> body
    ) {
        String providerCode = body.get("providerCode");
        boolean success = thirdPartyService.switchProvider(serviceType, providerCode);
        return success ? R.ok() : R.fail("切换提供商失败");
    }

    /**
     * 新增服务提供商
     */
    @SaCheckPermission("system:thirdparty:add")
    @PostMapping("/providers/{serviceType}")
    public R<Void> addProvider(
            @PathVariable String serviceType,
            @RequestBody SysThirdPartyProvider provider
    ) {
        provider.setServiceType(serviceType);
        boolean success = thirdPartyService.addProvider(provider);
        return success ? R.ok() : R.fail("新增提供商失败");
    }

    @SaCheckPermission("system:thirdparty:edit")
    @PutMapping("/providers/{providerId}")
    public R<Void> updateProvider(
            @PathVariable Long providerId,
            @RequestBody SysThirdPartyProvider provider
    ) {
        provider.setProviderId(providerId);
        boolean success = thirdPartyService.updateProvider(provider);
        return success ? R.ok() : R.fail("更新提供商失败");
    }

    @SaCheckPermission("system:thirdparty:remove")
    @DeleteMapping("/providers/{providerId}")
    public R<Void> deleteProvider(@PathVariable Long providerId) {
        boolean success = thirdPartyService.deleteProvider(providerId);
        return success ? R.ok() : R.fail("删除提供商失败");
    }

    /**
     * 新增提供商并批量创建配置
     */
    @SaCheckPermission("system:thirdparty:add")
    @PostMapping("/providers/{serviceType}/with-configs")
    public R<Void> addProviderWithConfigs(
            @PathVariable String serviceType,
            @RequestBody Map<String, Object> body
    ) {
        // 解析提供商信息
        @SuppressWarnings("unchecked")
        Map<String, Object> providerData = (Map<String, Object>) body.get("provider");
        SysThirdPartyProvider provider = new SysThirdPartyProvider();
        provider.setServiceType(serviceType);
        provider.setProviderCode((String) providerData.get("providerCode"));
        provider.setProviderName((String) providerData.get("providerName"));
        provider.setProviderLogo((String) providerData.get("providerLogo"));
        provider.setOfficialWebsite((String) providerData.get("officialWebsite"));
        provider.setDocUrl((String) providerData.get("docUrl"));
        provider.setRemark((String) providerData.get("remark"));
        
        // 先新增提供商
        boolean success = thirdPartyService.addProvider(provider);
        if (!success) {
            return R.fail("新增提供商失败");
        }
        
        // 解析配置项
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configsData = (List<Map<String, Object>>) body.get("configs");
        if (configsData != null && !configsData.isEmpty()) {
            List<SysThirdPartyConfigItem> configs = configsData.stream().map(configData -> {
                SysThirdPartyConfigItem config = new SysThirdPartyConfigItem();
                config.setServiceType(serviceType);
                config.setProvider(provider.getProviderCode());
                config.setConfigKey((String) configData.get("configKey"));
                config.setDefaultValue((String) configData.get("defaultValue"));
                config.setConfigValue((String) configData.get("defaultValue")); // 默认使用默认值
                config.setValueType((String) configData.get("valueType"));
                config.setSelectOptions((String) configData.get("selectOptions"));
                
                Object isRequired = configData.get("isRequired");
                config.setIsRequired(isRequired instanceof Boolean ? ((Boolean) isRequired ? 1 : 0) : 
                                    (isRequired instanceof Integer ? (Integer) isRequired : 0));
                
                Object isSensitive = configData.get("isSensitive");
                config.setIsSensitive(isSensitive instanceof Boolean ? ((Boolean) isSensitive ? 1 : 0) : 
                                     (isSensitive instanceof Integer ? (Integer) isSensitive : 0));
                
                config.setIsEnabled(1);
                
                Object displayOrder = configData.get("displayOrder");
                config.setDisplayOrder(displayOrder instanceof Integer ? (Integer) displayOrder : 1);
                
                config.setConfigLabel((String) configData.get("configLabel"));
                config.setHelpText((String) configData.get("helpText"));
                
                return config;
            }).collect(java.util.stream.Collectors.toList());
            
            // 批量创建配置
            success = thirdPartyService.addConfigBatch(configs);
            if (!success) {
                return R.fail("创建配置项失败");
            }
        }
        
        return R.ok();
    }

    /**
     * 获取指定服务的配置(所有提供商)
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/configs/{serviceType}")
    public R<Map<String, Object>> getServiceConfigs(@PathVariable String serviceType) {
        Map<String, Object> result = new HashMap<>();

        // 当前提供商
        SysThirdPartyProvider currentProvider = thirdPartyService.getCurrentProvider(serviceType);
        result.put("currentProvider", currentProvider);

        // 所有提供商
        List<SysThirdPartyProvider> providers = thirdPartyService.listProviders(serviceType);
        result.put("providers", providers);

        // 所有配置项(按提供商分组)
        Map<String, List<SysThirdPartyConfigItem>> configs = thirdPartyService.listAllConfigs(serviceType);
        result.put("configs", configs);

        return R.ok(result);
    }

    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/route-rules")
    public R<List<SysThirdPartyRouteRule>> listRouteRules(@RequestParam(required = false) String serviceType) {
        return R.ok(thirdPartyService.listRouteRules(serviceType));
    }

    @SaCheckPermission("system:thirdparty:add")
    @PostMapping("/route-rules")
    public R<Void> addRouteRule(@RequestBody SysThirdPartyRouteRule routeRule) {
        boolean success = thirdPartyService.addRouteRule(routeRule);
        return success ? R.ok() : R.fail("新增路由规则失败");
    }

    @SaCheckPermission("system:thirdparty:edit")
    @PutMapping("/route-rules/{routeRuleId}")
    public R<Void> updateRouteRule(
            @PathVariable Long routeRuleId,
            @RequestBody SysThirdPartyRouteRule routeRule
    ) {
        routeRule.setRouteRuleId(routeRuleId);
        boolean success = thirdPartyService.updateRouteRule(routeRule);
        return success ? R.ok() : R.fail("更新路由规则失败");
    }

    @SaCheckPermission("system:thirdparty:remove")
    @DeleteMapping("/route-rules/{routeRuleId}")
    public R<Void> deleteRouteRule(@PathVariable Long routeRuleId) {
        boolean success = thirdPartyService.deleteRouteRule(routeRuleId);
        return success ? R.ok() : R.fail("删除路由规则失败");
    }

    /**
     * 获取指定提供商的配置
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/configs/{serviceType}/{provider}")
    public R<List<SysThirdPartyConfigItem>> getProviderConfigs(
            @PathVariable String serviceType,
            @PathVariable String provider
    ) {
        return R.ok(thirdPartyService.listConfigs(serviceType, provider));
    }

    /**
     * 更新单个配置
     */
    @SaCheckPermission("system:thirdparty:edit")
    @PutMapping("/configs/{configId}")
    public R<Void> updateConfig(
            @PathVariable Long configId,
            @RequestBody Map<String, String> body
    ) {
        String configValue = body.get("configValue");
        boolean success = thirdPartyService.updateConfig(configId, configValue);
        return success ? R.ok() : R.fail("更新配置失败");
    }
    
    /**
     * 新增配置项
     */
    @SaCheckPermission("system:thirdparty:add")
    @PostMapping("/configs")
    public R<Void> addConfig(@RequestBody SysThirdPartyConfigItem config) {
        boolean success = thirdPartyService.addConfig(config);
        return success ? R.ok() : R.fail("新增配置失败");
    }
    
    /**
     * 删除配置项
     */
    @SaCheckPermission("system:thirdparty:remove")
    @DeleteMapping("/configs/{configId}")
    public R<Void> deleteConfig(@PathVariable Long configId) {
        boolean success = thirdPartyService.deleteConfig(configId);
        return success ? R.ok() : R.fail("删除配置失败");
    }

    /**
     * 批量更新配置
     */
    @SaCheckPermission("system:thirdparty:edit")
    @PutMapping("/configs/batch")
    public R<Void> updateConfigBatch(@RequestBody List<SysThirdPartyConfigItem> configs) {
        boolean success = thirdPartyService.updateConfigBatch(configs);
        return success ? R.ok() : R.fail("批量更新配置失败");
    }

    /**
     * 测试服务连接
     */
    @SaCheckPermission("system:thirdparty:test")
    @PostMapping("/test/{serviceType}/{provider}")
    public R<String> testConnection(
            @PathVariable String serviceType,
            @PathVariable String provider,
            @RequestBody Map<String, String> configs
    ) {
        try {
            String result = thirdPartyService.testConnection(serviceType, provider, configs);
            return R.ok(result);
        } catch (Exception e) {
            log.error("测试服务连接失败", e);
            return R.fail("连接测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个配置详情(需要查看敏感信息权限)
     */
    @SaCheckPermission("system:thirdparty:sensitive")
    @GetMapping("/configs/detail/{configId}")
    public R<SysThirdPartyConfigItem> getConfigDetail(@PathVariable Long configId) {
        SysThirdPartyConfigItem config = thirdPartyService.getConfigById(configId, true);
        return R.ok(config);
    }

    /**
     * AI 视频运行态概览
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/video/runtime")
    public R<Map<String, Object>> getVideoRuntimeOverview() {
        Map<String, Object> result = new HashMap<>();
        SysThirdPartyProvider currentProvider = null;
        try {
            currentProvider = thirdPartyService.getCurrentProvider("video");
        } catch (Exception e) {
            log.warn("读取 AI 视频当前服务商失败: {}", e.getMessage());
        }

        String defaultProviderCode = getStringProperty("shejiao.ai-video.provider", "doubao").toLowerCase();
        String currentProviderCode = currentProvider != null && StringUtils.isNotBlank(currentProvider.getProviderCode())
                ? currentProvider.getProviderCode().toLowerCase()
                : defaultProviderCode;

        result.put("enabled", getBooleanProperty("shejiao.ai-video.enabled", true));
        result.put("defaultProviderCode", defaultProviderCode);
        result.put("currentProviderCode", currentProviderCode);
        result.put("currentProviderName", currentProvider != null ? currentProvider.getProviderName() : currentProviderCode);
        result.put("allowMockFallback", getBooleanProperty("shejiao.ai-video.allow-mock-fallback", false));
        boolean quartzAvailable = hasQuartzScheduler();

        result.put("pollingMode", "quartz");
        result.put("pollingIntervalMs", null);
        result.put("pollingInitialDelayMs", null);
        result.put("pollingTimeoutMinutes", getIntegerProperty("shejiao.ai-video.polling.timeout-minutes", 15));
        result.put("springSchedulerActive", false);
        result.put("quartzRuntime", Map.of(
                "exists", quartzAvailable,
                "enabled", quartzAvailable,
                "jobCode", "ai_video_polling"
        ));
        result.put("recommendedProviderCode", "doubao");

        Map<String, String> providerAdvice = new HashMap<>();
        providerAdvice.put("doubao", "推荐优先打通生产链路");
        providerAdvice.put("jimeng", "保留候选，需要签名链路稳定后再切换");
        providerAdvice.put("kling", "后端 Provider 尚未接入完成");
        result.put("providerAdvice", providerAdvice);
        return R.ok(result);
    }

    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/runtime/traces")
    public R<Map<String, Object>> listRuntimeTraces(
            @RequestParam(required = false) String agentType,
            @RequestParam(required = false) Integer ownerUserId,
            @RequestParam(required = false) Integer targetUserId,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (!isAgentRuntimeEnabled()) {
            return R.ok(buildEmptyTracePage(page, pageSize));
        }
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(getAgentRuntimeBaseUrl() + "/runtime/traces")
                .queryParam("page", page)
                .queryParam("page_size", pageSize);
        if (StringUtils.isNotBlank(agentType)) {
            builder.queryParam("agent_type", agentType);
        }
        if (ownerUserId != null) {
            builder.queryParam("owner_user_id", ownerUserId);
        }
        if (targetUserId != null) {
            builder.queryParam("target_user_id", targetUserId);
        }
        if (StringUtils.isNotBlank(providerCode)) {
            builder.queryParam("provider_code", providerCode);
        }
        if (StringUtils.isNotBlank(traceId)) {
            builder.queryParam("trace_id", traceId);
        }
        if (StringUtils.isNotBlank(keyword)) {
            builder.queryParam("keyword", keyword);
        }
        JSONObject result = getRuntimeJson(builder.toUriString());
        return R.ok(result == null ? buildEmptyTracePage(page, pageSize) : result);
    }

    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/runtime/traces/{traceId}")
    public R<Map<String, Object>> getRuntimeTraceDetail(@PathVariable String traceId) {
        if (!isAgentRuntimeEnabled() || StringUtils.isBlank(traceId)) {
            return R.fail("Agent runtime 未启用或 traceId 为空");
        }
        JSONObject result = getRuntimeJson(getAgentRuntimeBaseUrl() + "/runtime/traces/" + traceId);
        return result == null ? R.fail("未查询到对应 trace") : R.ok(result);
    }

    /**
     * AI模型服务配置(快捷接口)
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/ai/config")
    public R<Map<String, Object>> getAIConfig() {
        return getServiceConfigs("ai");
    }

    /**
     * 云存储服务配置(快捷接口)
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/oss/config")
    public R<Map<String, Object>> getOSSConfig() {
        return getServiceConfigs("oss");
    }

    /**
     * 短信服务配置(快捷接口)
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/sms/config")
    public R<Map<String, Object>> getSMSConfig() {
        return getServiceConfigs("sms");
    }

    /**
     * 支付服务配置(快捷接口)
     */
    @SaCheckPermission("system:thirdparty:query")
    @GetMapping("/payment/config")
    public R<Map<String, Object>> getPaymentConfig() {
        return getServiceConfigs("payment");
    }

    private String getStringProperty(String key, String defaultValue) {
        return StringUtils.defaultIfBlank(environment.getProperty(key), defaultValue);
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(environment.getProperty(key, String.valueOf(defaultValue)));
    }

    private long getLongProperty(String key, long defaultValue) {
        return Long.parseLong(environment.getProperty(key, String.valueOf(defaultValue)));
    }

    private int getIntegerProperty(String key, int defaultValue) {
        return Integer.parseInt(environment.getProperty(key, String.valueOf(defaultValue)));
    }

    private boolean hasQuartzScheduler() {
        try {
            Class<?> schedulerClass = Class.forName("org.quartz.Scheduler");
            return applicationContext.getBeanNamesForType(schedulerClass).length > 0;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isAgentRuntimeEnabled() {
        return getBusinessConfigBoolean("agent.runtime.enabled", true)
                && StringUtils.isNotBlank(getAgentRuntimeBaseUrl());
    }

    private String getAgentRuntimeBaseUrl() {
        String value = getBusinessConfigString("agent.runtime.baseUrl", "http://127.0.0.1:8091");
        return org.apache.commons.lang3.StringUtils.removeEnd(StringUtils.defaultIfBlank(value, "http://127.0.0.1:8091"), "/");
    }

    private RestTemplate getRuntimeAdminRestTemplate() {
        RestTemplate local = runtimeAdminRestTemplate;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (runtimeAdminRestTemplate == null) {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(5000);
                factory.setReadTimeout(20000);
                runtimeAdminRestTemplate = new RestTemplate(factory);
            }
            return runtimeAdminRestTemplate;
        }
    }

    private JSONObject getRuntimeJson(String url) {
        try {
            ResponseEntity<String> response = getRuntimeAdminRestTemplate().getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || StringUtils.isBlank(response.getBody())) {
                return null;
            }
            return JSON.parseObject(response.getBody());
        } catch (Exception ex) {
            log.warn("读取 runtime 调试信息失败, url={}, err={}", url, ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildEmptyTracePage(Integer page, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", 0);
        result.put("page", page == null || page < 1 ? 1 : page);
        result.put("page_size", pageSize == null || pageSize < 1 ? 20 : pageSize);
        result.put("items", List.of());
        return result;
    }

    private String getBusinessConfigString(String key, String defaultValue) {
        try {
            Class<?> configType = Class.forName("org.aileme.shejiao.api.service.PlatformBusinessConfigService");
            Object service = applicationContext.getBean(configType);
            Object value = configType.getMethod("getString", String.class, String.class).invoke(service, key, defaultValue);
            return value == null ? defaultValue : String.valueOf(value);
        } catch (Exception ignored) {
            return environment.getProperty(key, defaultValue);
        }
    }

    private boolean getBusinessConfigBoolean(String key, boolean defaultValue) {
        try {
            Class<?> configType = Class.forName("org.aileme.shejiao.api.service.PlatformBusinessConfigService");
            Object service = applicationContext.getBean(configType);
            Object value = configType.getMethod("getBoolean", String.class, Boolean.class).invoke(service, key, defaultValue);
            if (value instanceof Boolean bool) {
                return bool;
            }
            return value != null ? Boolean.parseBoolean(String.valueOf(value)) : defaultValue;
        } catch (Exception ignored) {
            return Boolean.parseBoolean(environment.getProperty(key, String.valueOf(defaultValue)));
        }
    }
}
