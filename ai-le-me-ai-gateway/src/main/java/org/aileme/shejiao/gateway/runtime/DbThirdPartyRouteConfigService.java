package org.aileme.shejiao.gateway.runtime;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于数据库的运行时配置读取
 */
@Service
@DS("master")
public class DbThirdPartyRouteConfigService implements ThirdPartyRouteConfigService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DbThirdPartyRouteConfigService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getCurrentProviderCode(String serviceType, String fallbackCode) {
        String sql = "SELECT provider_code " +
            "FROM sys_third_party_provider " +
            "WHERE service_type = ? AND is_current = 1 AND is_enabled = 1 " +
            "ORDER BY display_order ASC, provider_id ASC LIMIT 1";
        List<String> providers = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> StringUtils.trimToEmpty(rs.getString("provider_code")),
            serviceType
        );
        if (providers != null && !providers.isEmpty() && StringUtils.isNotBlank(providers.get(0))) {
            return providers.get(0).toLowerCase();
        }
        return StringUtils.defaultIfBlank(fallbackCode, "").toLowerCase();
    }

    @Override
    public Map<String, String> getProviderConfigs(String serviceType, String providerCode) {
        return getProviderConfigs(serviceType, providerCode, null);
    }

    @Override
    public Map<String, String> getProviderConfigs(String serviceType, String providerCode, String profileCode) {
        Map<String, String> configs = new LinkedHashMap<>();
        if (StringUtils.isAnyBlank(serviceType, providerCode)) {
            return configs;
        }

        ProviderConfigDocument providerDocument = loadProviderConfigDocument(serviceType, providerCode);
        if (providerDocument != null) {
            configs.putAll(providerDocument.resolveProfileConfigs(profileCode));
        }
        if (!"common".equalsIgnoreCase(providerCode)) {
            ProviderConfigDocument commonDocument = loadProviderConfigDocument(serviceType, "common");
            if (commonDocument != null) {
                commonDocument.resolveProfileConfigs(profileCode).forEach(configs::putIfAbsent);
            }
        }
        return configs;
    }

    @Override
    public String getConfigValue(String serviceType, String providerCode, String configKey) {
        if (StringUtils.isAnyBlank(serviceType, providerCode, configKey)) {
            return "";
        }
        Map<String, String> configs = getProviderConfigs(serviceType, providerCode);
        String value = configs.get(configKey);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        // 兼容公共配置（provider=common）
        if (!"common".equalsIgnoreCase(providerCode)) {
            Map<String, String> common = getProviderConfigs(serviceType, "common");
            return StringUtils.defaultIfBlank(common.get(configKey), "");
        }
        return "";
    }

    @Override
    public String getConfigValueForCurrentProvider(String serviceType, String configKey, String fallbackProvider) {
        String provider = getCurrentProviderCode(serviceType, fallbackProvider);
        if (StringUtils.isBlank(provider)) {
            return "";
        }
        return getConfigValue(serviceType, provider, configKey);
    }

    @Override
    public ThirdPartyResolvedRoute resolveRoute(String serviceType,
                                                Map<String, String> routeContext,
                                                String fallbackProvider) {
        ThirdPartyResolvedRoute route = new ThirdPartyResolvedRoute();
        route.setServiceType(serviceType);

        String currentProvider = getCurrentProviderCode(serviceType, fallbackProvider);
        List<RouteRuleRow> rules = loadRouteRules(serviceType);
        for (RouteRuleRow rule : rules) {
            if (!rule.matches(routeContext)) {
                continue;
            }
            String resolvedProvider = normalizeCode(StringUtils.defaultIfBlank(rule.providerCode, currentProvider));
            route.setProviderCode(resolvedProvider);
            route.setProfileCode(normalizeCode(rule.profileCode));
            route.setRouteRuleId(rule.routeRuleId);
            route.getConfigs().putAll(getProviderConfigs(serviceType, resolvedProvider, route.getProfileCode()));
            if (StringUtils.isBlank(route.getProfileCode())) {
                route.setProfileCode(normalizeCode(route.getConfigs().get("profile_code")));
            }
            return route;
        }

        route.setProviderCode(currentProvider);
        route.getConfigs().putAll(getProviderConfigs(serviceType, currentProvider));
        route.setProfileCode(normalizeCode(route.getConfigs().get("profile_code")));
        return route;
    }

    private ProviderConfigDocument loadProviderConfigDocument(String serviceType, String providerCode) {
        String sql = "SELECT config_json FROM sys_third_party_provider " +
            "WHERE service_type = ? AND provider_code = ? AND is_enabled = 1 LIMIT 1";
        List<String> values = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> StringUtils.trimToEmpty(rs.getString("config_json")),
            serviceType,
            providerCode
        );
        if (values == null || values.isEmpty() || StringUtils.isBlank(values.get(0))) {
            return null;
        }
        try {
            return ProviderConfigDocument.fromJson(objectMapper, values.get(0));
        } catch (Exception e) {
            throw new IllegalStateException(
                String.format("第三方服务配置JSON解析失败: serviceType=%s, provider=%s", serviceType, providerCode),
                e
            );
        }
    }

    private List<RouteRuleRow> loadRouteRules(String serviceType) {
        String sql = "SELECT route_rule_id, scene_code, template_code, content_mode, function_type, provider_code, profile_code, match_json " +
            "FROM sys_third_party_route_rule " +
            "WHERE service_type = ? AND is_enabled = 1 " +
            "ORDER BY priority ASC, route_rule_id ASC";
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new RouteRuleRow(
                rs.getLong("route_rule_id"),
                trimToNull(rs.getString("scene_code")),
                trimToNull(rs.getString("template_code")),
                trimToNull(rs.getString("content_mode")),
                trimToNull(rs.getString("function_type")),
                trimToNull(rs.getString("provider_code")),
                trimToNull(rs.getString("profile_code")),
                trimToNull(rs.getString("match_json"))
            ),
            serviceType
        );
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    private String normalizeCode(String value) {
        return StringUtils.lowerCase(StringUtils.trimToEmpty(value)).replace('-', '_');
    }

    private final class RouteRuleRow {
        private final Long routeRuleId;
        private final String sceneCode;
        private final String templateCode;
        private final String contentMode;
        private final String functionType;
        private final String providerCode;
        private final String profileCode;
        private final String matchJson;

        private RouteRuleRow(Long routeRuleId,
                             String sceneCode,
                             String templateCode,
                             String contentMode,
                             String functionType,
                             String providerCode,
                             String profileCode,
                             String matchJson) {
            this.routeRuleId = routeRuleId;
            this.sceneCode = sceneCode;
            this.templateCode = templateCode;
            this.contentMode = contentMode;
            this.functionType = functionType;
            this.providerCode = providerCode;
            this.profileCode = profileCode;
            this.matchJson = matchJson;
        }

        private boolean matches(Map<String, String> routeContext) {
            Map<String, String> context = routeContext == null ? Collections.emptyMap() : routeContext;
            if (!matchesExact("scene_code", sceneCode, context)) {
                return false;
            }
            if (!matchesExact("template_code", templateCode, context)) {
                return false;
            }
            if (!matchesExact("content_mode", contentMode, context)) {
                return false;
            }
            if (!matchesExact("function_type", functionType, context)) {
                return false;
            }
            if (StringUtils.isBlank(matchJson)) {
                return true;
            }
            try {
                Map<String, Object> extraMatch = objectMapper.readValue(matchJson, new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : extraMatch.entrySet()) {
                    String actual = StringUtils.trimToEmpty(context.get(entry.getKey()));
                    Object expected = entry.getValue();
                    if (expected instanceof List<?> list) {
                        boolean matched = list.stream().filter(Objects::nonNull)
                            .map(String::valueOf)
                            .map(StringUtils::trimToEmpty)
                            .anyMatch(item -> StringUtils.equalsIgnoreCase(item, actual));
                        if (!matched) {
                            return false;
                        }
                        continue;
                    }
                    if (!StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(String.valueOf(expected)), actual)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                throw new IllegalStateException("第三方路由规则 match_json 解析失败: ruleId=" + routeRuleId, e);
            }
        }

        private boolean matchesExact(String key, String expected, Map<String, String> context) {
            if (StringUtils.isBlank(expected)) {
                return true;
            }
            String actual = StringUtils.trimToEmpty(context.get(key));
            return StringUtils.equalsIgnoreCase(expected, actual);
        }
    }

    private static final class ProviderConfigDocument {
        private final Map<String, String> rootConfigs;
        private final Map<String, Map<String, String>> profiles;
        private final String defaultProfileCode;

        private ProviderConfigDocument(Map<String, String> rootConfigs,
                                       Map<String, Map<String, String>> profiles,
                                       String defaultProfileCode) {
            this.rootConfigs = rootConfigs;
            this.profiles = profiles;
            this.defaultProfileCode = defaultProfileCode;
        }

        private static ProviderConfigDocument fromJson(ObjectMapper objectMapper, String json) throws Exception {
            JsonNode root = objectMapper.readTree(json);
            Map<String, String> rootConfigs = new LinkedHashMap<>();
            Map<String, Map<String, String>> profiles = new LinkedHashMap<>();

            String defaultProfileCode = "";
            if (root.hasNonNull("defaultProfile")) {
                defaultProfileCode = normalizeStatic(root.get("defaultProfile").asText());
            } else if (root.hasNonNull("default_profile")) {
                defaultProfileCode = normalizeStatic(root.get("default_profile").asText());
            }

            root.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if ("profiles".equals(key) || "defaultProfile".equals(key) || "default_profile".equals(key)) {
                    return;
                }
                if (entry.getValue().isContainerNode()) {
                    return;
                }
                putConfigValue(rootConfigs, key, entry.getValue().asText(""));
            });

            JsonNode profilesNode = root.get("profiles");
            if (profilesNode != null && profilesNode.isArray()) {
                profilesNode.forEach(item -> {
                    if (!item.isObject()) {
                        return;
                    }
                    String code = normalizeStatic(readText(item, "code", "profileCode", "profile_code"));
                    if (StringUtils.isBlank(code)) {
                        return;
                    }
                    Map<String, String> profileConfig = new LinkedHashMap<>();
                    item.fields().forEachRemaining(field -> {
                        if ("code".equals(field.getKey()) || "profileCode".equals(field.getKey()) || "profile_code".equals(field.getKey())) {
                            return;
                        }
                        if (field.getValue().isContainerNode()) {
                            return;
                        }
                        putConfigValue(profileConfig, field.getKey(), field.getValue().asText(""));
                    });
                    putConfigValue(profileConfig, "profile_code", code);
                    profiles.put(code, profileConfig);
                });
            }

            if (StringUtils.isBlank(defaultProfileCode) && !profiles.isEmpty()) {
                defaultProfileCode = profiles.keySet().iterator().next();
            }
            return new ProviderConfigDocument(rootConfigs, profiles, defaultProfileCode);
        }

        private Map<String, String> resolveProfileConfigs(String requestedProfileCode) {
            Map<String, String> resolved = new LinkedHashMap<>(rootConfigs);
            String profileCode = normalizeStatic(StringUtils.defaultIfBlank(requestedProfileCode, defaultProfileCode));
            if (StringUtils.isNotBlank(profileCode) && profiles.containsKey(profileCode)) {
                resolved.putAll(profiles.get(profileCode));
            }
            if (StringUtils.isNotBlank(profileCode)) {
                resolved.put("profile_code", profileCode);
            }
            return resolved;
        }

        private static void putConfigValue(Map<String, String> target, String key, String value) {
            String trimmed = StringUtils.trimToEmpty(value);
            target.put(key, trimmed);
            target.put(normalizeJsonKey(key), trimmed);
        }

        private static String readText(JsonNode node, String... keys) {
            for (String key : keys) {
                JsonNode value = node.get(key);
                if (value != null && !value.isNull()) {
                    return value.asText("");
                }
            }
            return "";
        }

        private static String normalizeJsonKey(String key) {
            if (StringUtils.isBlank(key)) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (char ch : key.toCharArray()) {
                if (Character.isUpperCase(ch)) {
                    builder.append('_').append(Character.toLowerCase(ch));
                } else {
                    builder.append(Character.toLowerCase(ch));
                }
            }
            return builder.toString();
        }

        private static String normalizeStatic(String value) {
            return StringUtils.lowerCase(StringUtils.trimToEmpty(value)).replace('-', '_');
        }
    }
}
