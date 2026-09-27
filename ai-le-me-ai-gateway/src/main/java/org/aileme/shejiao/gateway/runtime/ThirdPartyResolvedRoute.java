package org.aileme.shejiao.gateway.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方服务运行时路由结果
 */
public class ThirdPartyResolvedRoute {

    private String serviceType;
    private String providerCode;
    private String profileCode;
    private Long routeRuleId;
    private final Map<String, String> configs = new LinkedHashMap<>();

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getProfileCode() {
        return profileCode;
    }

    public void setProfileCode(String profileCode) {
        this.profileCode = profileCode;
    }

    public Long getRouteRuleId() {
        return routeRuleId;
    }

    public void setRouteRuleId(Long routeRuleId) {
        this.routeRuleId = routeRuleId;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }
}
