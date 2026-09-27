package org.aileme.shejiao.api.service;

import com.alibaba.fastjson.JSONObject;

/**
 * 第三方运行时配置统一读取入口。
 *
 * 约束：只读取 sys_third_party_provider.config_json
 */
public interface ThirdPartyRuntimeConfigService {

    String getProviderCode(String serviceType, String defaultValue, String... legacyBusinessKeys);

    String getConfig(String serviceType, String providerCode, String... candidateKeys);

    String getCurrentConfig(String serviceType, String... candidateKeys);

    Integer getInt(String serviceType, String providerCode, Integer defaultValue, String... candidateKeys);

    Boolean getBoolean(String serviceType, String providerCode, Boolean defaultValue, String... candidateKeys);

    JSONObject getProviderConfigJson(String serviceType, String providerCode);
}
