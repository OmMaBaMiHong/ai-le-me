package org.aileme.shejiao.app.runtime.thirdparty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.aileme.system.domain.SysThirdPartyProvider;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

import java.util.Objects;

/**
 * 第三方运行时配置统一门面。
 * 只读取 sys_third_party_provider.config_json。
 */
@Service
public class ThirdPartyRuntimeConfigServiceImpl implements ThirdPartyRuntimeConfigService {

    private final ISysThirdPartyService thirdPartyService;

    public ThirdPartyRuntimeConfigServiceImpl(ISysThirdPartyService thirdPartyService) {
        this.thirdPartyService = thirdPartyService;
    }

    @Override
    public String getProviderCode(String serviceType, String defaultValue, String... legacyBusinessKeys) {
        SysThirdPartyProvider currentProvider = thirdPartyService.getCurrentProvider(serviceType);
        if (currentProvider != null && StringUtils.isNotBlank(currentProvider.getProviderCode())) {
            return currentProvider.getProviderCode();
        }
        return defaultValue;
    }

    @Override
    public String getConfig(String serviceType, String providerCode, String... candidateKeys) {
        if (candidateKeys == null || candidateKeys.length == 0) {
            return null;
        }
        SysThirdPartyProvider provider = resolveProvider(serviceType, providerCode);
        JSONObject providerConfig = parseConfigJson(provider);
        for (String candidateKey : candidateKeys) {
            if (StringUtils.isBlank(candidateKey)) {
                continue;
            }
            String jsonValue = readJsonValue(providerConfig, candidateKey);
            if (StringUtils.isNotBlank(jsonValue)) {
                return jsonValue;
            }
            if (!"common".equalsIgnoreCase(providerCode)) {
                JSONObject commonConfig = parseConfigJson(thirdPartyService.getProvider(serviceType, "common"));
                String commonValue = readJsonValue(commonConfig, candidateKey);
                if (StringUtils.isNotBlank(commonValue)) {
                    return commonValue;
                }
            }
        }
        return null;
    }

    @Override
    public String getCurrentConfig(String serviceType, String... candidateKeys) {
        String providerCode = getProviderCode(serviceType, null);
        return getConfig(serviceType, providerCode, candidateKeys);
    }

    @Override
    public Integer getInt(String serviceType, String providerCode, Integer defaultValue, String... candidateKeys) {
        String value = getConfig(serviceType, providerCode, candidateKeys);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBoolean(String serviceType, String providerCode, Boolean defaultValue, String... candidateKeys) {
        String value = getConfig(serviceType, providerCode, candidateKeys);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        String normalized = value.trim();
        if ("1".equals(normalized) || "true".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("0".equals(normalized) || "false".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        return defaultValue;
    }

    @Override
    public JSONObject getProviderConfigJson(String serviceType, String providerCode) {
        return parseConfigJson(resolveProvider(serviceType, providerCode));
    }

    private SysThirdPartyProvider resolveProvider(String serviceType, String providerCode) {
        if (StringUtils.isBlank(serviceType)) {
            return null;
        }
        if (StringUtils.isBlank(providerCode)) {
            return thirdPartyService.getCurrentProvider(serviceType);
        }
        SysThirdPartyProvider provider = thirdPartyService.getProvider(serviceType, providerCode);
        if (provider != null) {
            return provider;
        }
        return null;
    }

    private JSONObject parseConfigJson(SysThirdPartyProvider provider) {
        if (provider == null || StringUtils.isBlank(provider.getConfigJson())) {
            return null;
        }
        try {
            return JSON.parseObject(provider.getConfigJson());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readJsonValue(JSONObject configJson, String candidateKey) {
        if (configJson == null || StringUtils.isBlank(candidateKey)) {
            return null;
        }
        Object directValue = configJson.get(candidateKey);
        if (directValue != null && StringUtils.isNotBlank(String.valueOf(directValue))) {
            return String.valueOf(directValue);
        }
        Object relaxedValue = configJson.get(toSnakeCase(candidateKey));
        if (relaxedValue != null && StringUtils.isNotBlank(String.valueOf(relaxedValue))) {
            return String.valueOf(relaxedValue);
        }
        Object profileValue = readFromProfiles(configJson, candidateKey);
        if (profileValue != null && StringUtils.isNotBlank(String.valueOf(profileValue))) {
            return String.valueOf(profileValue);
        }
        return null;
    }

    private Object readFromProfiles(JSONObject configJson, String candidateKey) {
        Object profilesObj = configJson.get("profiles");
        if (!(profilesObj instanceof JSONArray profiles) || profiles.isEmpty()) {
            return null;
        }
        String defaultProfile = configJson.getString("defaultProfile");
        for (int i = 0; i < profiles.size(); i++) {
            JSONObject profile = profiles.getJSONObject(i);
            if (profile == null) {
                continue;
            }
            if (StringUtils.isNotBlank(defaultProfile) && !StringUtils.equals(defaultProfile, profile.getString("code"))) {
                continue;
            }
            Object directValue = profile.get(candidateKey);
            if (directValue != null) {
                return directValue;
            }
            Object relaxedValue = profile.get(toSnakeCase(candidateKey));
            if (relaxedValue != null) {
                return relaxedValue;
            }
        }
        return null;
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
