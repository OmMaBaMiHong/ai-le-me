package org.aileme.system.service;

import org.aileme.system.domain.SysThirdPartyConfigItem;
import org.aileme.system.domain.SysThirdPartyProvider;
import org.aileme.system.domain.SysThirdPartyRouteRule;

import java.util.List;
import java.util.Map;

/**
 * 第三方服务配置Service接口
 *
 * @author system
 * @date 2026-02-14
 */
public interface ISysThirdPartyService {

    /**
     * 获取指定服务类型的所有提供商
     *
     * @param serviceType 服务类型
     * @return 提供商列表
     */
    List<SysThirdPartyProvider> listProviders(String serviceType);

    /**
     * 获取指定服务类型的当前提供商
     *
     * @param serviceType 服务类型
     * @return 当前提供商
     */
    SysThirdPartyProvider getCurrentProvider(String serviceType);

    /**
     * 获取指定服务类型下的某个提供商
     *
     * @param serviceType 服务类型
     * @param providerCode 提供商编码
     * @return 提供商
     */
    SysThirdPartyProvider getProvider(String serviceType, String providerCode);

    /**
     * 切换服务提供商
     *
     * @param serviceType  服务类型
     * @param providerCode 提供商代码
     * @return 是否成功
     */
    boolean switchProvider(String serviceType, String providerCode);

    /**
     * 获取指定提供商的所有配置项
     *
     * @param serviceType 服务类型
     * @param provider    提供商
     * @return 配置项列表
     */
    List<SysThirdPartyConfigItem> listConfigs(String serviceType, String provider);

    /**
     * 获取指定服务的所有配置(包含多个提供商)
     *
     * @param serviceType 服务类型
     * @return 分组后的配置Map, key=provider, value=配置列表
     */
    Map<String, List<SysThirdPartyConfigItem>> listAllConfigs(String serviceType);

    /**
     * 获取单个配置项的值
     *
     * @param serviceType 服务类型
     * @param provider    提供商
     * @param configKey   配置键
     * @return 配置值
     */
    String getConfigValue(String serviceType, String provider, String configKey);

    /**
     * 获取单个配置项的值(使用当前提供商)
     *
     * @param serviceType 服务类型
     * @param configKey   配置键
     * @return 配置值
     */
    String getConfigValue(String serviceType, String configKey);

    /**
     * 更新配置项
     *
     * @param configId    配置ID
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean updateConfig(Long configId, String configValue);

    /**
     * 批量更新配置项
     *
     * @param configs 配置列表
     * @return 是否成功
     */
    boolean updateConfigBatch(List<SysThirdPartyConfigItem> configs);

    /**
     * 测试服务连接
     *
     * @param serviceType 服务类型
     * @param provider    提供商
     * @param configs     临时配置(用于测试新配置)
     * @return 测试结果消息
     */
    String testConnection(String serviceType, String provider, Map<String, String> configs);

    /**
     * 获取配置项(脱敏处理)
     *
     * @param configId       配置ID
     * @param showSensitive  是否显示敏感信息
     * @return 配置项
     */
    SysThirdPartyConfigItem getConfigById(Long configId, boolean showSensitive);

    /**
     * 获取所有服务类型
     *
     * @return 服务类型列表
     */
    List<String> listServiceTypes();

    /**
     * 初始化运行期缓存
     */
    void initRuntimeCaches();

    /**
     * 新增配置项
     *
     * @param config 配置对象
     * @return 是否成功
     */
    boolean addConfig(SysThirdPartyConfigItem config);

    /**
     * 删除配置项
     *
     * @param configId 配置ID
     * @return 是否成功
     */
    boolean deleteConfig(Long configId);

    /**
     * 新增服务提供商
     *
     * @param provider 提供商对象
     * @return 是否成功
     */
    boolean addProvider(SysThirdPartyProvider provider);

    /**
     * 更新服务提供商
     */
    boolean updateProvider(SysThirdPartyProvider provider);

    /**
     * 删除服务提供商
     */
    boolean deleteProvider(Long providerId);

    /**
     * 批量新增配置项(用于新增提供商时批量创建配置)
     *
     * @param configs 配置列表
     * @return 是否成功
     */
    boolean addConfigBatch(List<SysThirdPartyConfigItem> configs);

    /**
     * 获取路由规则列表
     */
    List<SysThirdPartyRouteRule> listRouteRules(String serviceType);

    /**
     * 新增路由规则
     */
    boolean addRouteRule(SysThirdPartyRouteRule routeRule);

    /**
     * 更新路由规则
     */
    boolean updateRouteRule(SysThirdPartyRouteRule routeRule);

    /**
     * 删除路由规则
     */
    boolean deleteRouteRule(Long routeRuleId);
}
