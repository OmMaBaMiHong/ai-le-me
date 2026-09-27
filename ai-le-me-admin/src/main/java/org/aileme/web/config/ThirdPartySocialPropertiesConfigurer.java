package org.aileme.web.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.json.utils.JsonUtils;
import org.aileme.common.social.config.properties.SocialLoginConfigProperties;
import org.aileme.common.social.config.properties.SocialProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 社交登录源统一从三方 provider 表装配。
 *
 * 约束：
 * 1. 只启用 sys_third_party_provider 中已配置完成的 source
 * 2. 不再依赖 application-*.yml 中的 clientId/clientSecret 占位值
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThirdPartySocialPropertiesConfigurer {

    private static final List<SocialSourceBinding> SOURCE_BINDINGS = List.of(
        new SocialSourceBinding("wechat_mp", "wechat_mp", false),
        new SocialSourceBinding("wechat_open", "wechat_app", false),
        new SocialSourceBinding("qq", "social_qq", false),
        new SocialSourceBinding("weibo", "social_weibo", false),
        new SocialSourceBinding("gitee", "social_gitee", false),
        new SocialSourceBinding("dingtalk", "social_dingtalk", false),
        new SocialSourceBinding("baidu", "social_baidu", false),
        new SocialSourceBinding("csdn", "social_csdn", false),
        new SocialSourceBinding("coding", "social_coding", false),
        new SocialSourceBinding("oschina", "social_oschina", false),
        new SocialSourceBinding("alipay_wallet", "social_alipay_wallet", false),
        new SocialSourceBinding("gitlab", "social_gitlab", false),
        new SocialSourceBinding("gitea", "social_gitea", true),
        new SocialSourceBinding("maxkey", "social_maxkey", true),
        new SocialSourceBinding("topiam", "social_topiam", true),
        new SocialSourceBinding("github", "social_github", false),
        new SocialSourceBinding("douyin", "social_douyin", false),
        new SocialSourceBinding("linkedin", "social_linkedin", false),
        new SocialSourceBinding("microsoft", "social_microsoft", false),
        new SocialSourceBinding("renren", "social_renren", false),
        new SocialSourceBinding("stack_overflow", "social_stack_overflow", false),
        new SocialSourceBinding("huawei", "social_huawei", false),
        new SocialSourceBinding("aliyun", "social_aliyun", false),
        new SocialSourceBinding("taobao", "social_taobao", false),
        new SocialSourceBinding("wechat_enterprise", "social_wechat_enterprise", false)
    );

    private final SocialProperties socialProperties;
    private final ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService;

    @Value("${justauth.address:http://localhost:80}")
    private String justauthAddress;

    @PostConstruct
    public void configureSources() {
        Map<String, SocialLoginConfigProperties> configuredSources = new LinkedHashMap<>();
        List<String> enabledSources = new ArrayList<>();
        List<String> disabledSources = new ArrayList<>();

        for (SocialSourceBinding binding : SOURCE_BINDINGS) {
            SocialLoginConfigProperties properties = buildSource(binding);
            if (properties == null) {
                disabledSources.add(binding.source());
                continue;
            }
            configuredSources.put(binding.source(), properties);
            enabledSources.add(binding.source());
        }

        socialProperties.setType(configuredSources);
        log.info("社交登录源已从三方配置表装配完成, enabled={}, skipped={}", enabledSources, disabledSources);
    }

    private SocialLoginConfigProperties buildSource(SocialSourceBinding binding) {
        String providerCode = thirdPartyRuntimeConfigService.getProviderCode(binding.serviceType(), null);
        if (StringUtils.isBlank(providerCode)) {
            return null;
        }
        JSONObject providerConfig = thirdPartyRuntimeConfigService.getProviderConfigJson(binding.serviceType(), providerCode);
        if (providerConfig == null || providerConfig.isEmpty()) {
            return null;
        }

        String clientId = getStringValue(providerConfig,
            "client_id", "clientId", "app_id", "appid");
        String clientSecret = getStringValue(providerConfig,
            "client_secret", "clientSecret", "app_secret", "secret");
        if (StringUtils.isAnyBlank(clientId, clientSecret)) {
            log.info("跳过未配置完成的社交登录源: source={}, serviceType={}, provider={}",
                binding.source(), binding.serviceType(), providerCode);
            return null;
        }

        SocialLoginConfigProperties properties = new SocialLoginConfigProperties();
        properties.setClientId(clientId);
        properties.setClientSecret(clientSecret);
        properties.setRedirectUri(buildRedirectUri(binding.source()));
        properties.setUnionId(getBooleanValue(providerConfig, "union_id", "unionId"));
        properties.setTenantId(getStringValue(providerConfig, "tenant_id", "tenantId"));
        properties.setCodingGroupName(getStringValue(providerConfig, "coding_group_name", "codingGroupName"));
        properties.setAlipayPublicKey(getStringValue(providerConfig, "alipay_public_key", "alipayPublicKey"));
        properties.setAgentId(getStringValue(providerConfig, "agent_id", "agentId"));
        properties.setStackOverflowKey(getStringValue(providerConfig, "stack_overflow_key", "stackOverflowKey"));
        properties.setDeviceId(getStringValue(providerConfig, "device_id", "deviceId"));
        properties.setClientOsType(getStringValue(providerConfig, "client_os_type", "clientOsType"));
        properties.setServerUrl(getStringValue(providerConfig, "server_url", "serverUrl"));
        if (binding.requireServerUrl() && StringUtils.isBlank(properties.getServerUrl())) {
            log.info("跳过缺少 server_url 的社交登录源: source={}, serviceType={}, provider={}",
                binding.source(), binding.serviceType(), providerCode);
            return null;
        }
        properties.setScopes(resolveScopes(providerConfig));
        return properties;
    }

    private List<String> resolveScopes(JSONObject providerConfig) {
        Object scopesValue = getValue(providerConfig, "scopes");
        if (scopesValue == null) {
            return null;
        }
        if (scopesValue instanceof JSONArray jsonArray) {
            List<String> result = jsonArray.toJavaList(String.class);
            return result.isEmpty() ? null : result;
        }
        String scopes = String.valueOf(scopesValue);
        if (StringUtils.isBlank(scopes)) {
            return null;
        }
        if (JsonUtils.isJsonArray(scopes)) {
            List<String> result = JsonUtils.parseArray(scopes, String.class);
            return result.isEmpty() ? null : result;
        }
        List<String> result = new ArrayList<>();
        for (String item : scopes.split(",")) {
            String scope = StringUtils.trimToEmpty(item);
            if (StringUtils.isNotBlank(scope)) {
                result.add(scope);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String buildRedirectUri(String source) {
        return StringUtils.removeEnd(justauthAddress, "/") + "/social-callback?source=" + source;
    }

    private String getStringValue(JSONObject providerConfig, String... candidateKeys) {
        Object value = getValue(providerConfig, candidateKeys);
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value);
        return StringUtils.isBlank(result) ? null : result;
    }

    private Boolean getBooleanValue(JSONObject providerConfig, String... candidateKeys) {
        String value = getStringValue(providerConfig, candidateKeys);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if ("1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Object getValue(JSONObject providerConfig, String... candidateKeys) {
        if (providerConfig == null || candidateKeys == null) {
            return null;
        }
        for (String candidateKey : candidateKeys) {
            if (StringUtils.isBlank(candidateKey)) {
                continue;
            }
            Object directValue = providerConfig.get(candidateKey);
            if (directValue != null) {
                return directValue;
            }
            Object snakeCaseValue = providerConfig.get(toSnakeCase(candidateKey));
            if (snakeCaseValue != null) {
                return snakeCaseValue;
            }
        }
        return null;
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private record SocialSourceBinding(String source, String serviceType, boolean requireServerUrl) {
    }
}
