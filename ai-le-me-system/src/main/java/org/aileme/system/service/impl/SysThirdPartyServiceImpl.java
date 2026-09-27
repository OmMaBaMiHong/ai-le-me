package org.aileme.system.service.impl;

import cn.hutool.core.lang.Dict;
import cn.hutool.http.HttpUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.constant.CacheNames;
import org.aileme.common.json.utils.JsonUtils;
import org.aileme.common.oss.constant.OssConstant;
import org.aileme.common.oss.enums.AccessPolicyType;
import org.aileme.common.oss.properties.OssProperties;
import org.aileme.common.redis.utils.CacheUtils;
import org.aileme.common.redis.utils.RedisUtils;
import org.aileme.system.domain.SysThirdPartyConfigItem;
import org.aileme.system.domain.SysThirdPartyProvider;
import org.aileme.system.domain.SysThirdPartyRouteRule;
import org.aileme.system.mapper.SysThirdPartyProviderMapper;
import org.aileme.system.mapper.SysThirdPartyRouteRuleMapper;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 第三方服务配置Service实现
 *
 * 运行期只依赖 sys_third_party_provider.config_json 与 sys_third_party_route_rule。
 */
@Slf4j
@Service
@DS("master")
@RequiredArgsConstructor
public class SysThirdPartyServiceImpl implements ISysThirdPartyService {

    private static final String CACHE_KEY_PREFIX = "third_party:config:";
    private static final String CACHE_KEY_PROVIDER = "third_party:provider:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String COMMON_PROVIDER_CODE = "common";
    private static final Set<String> RESERVED_CONFIG_KEYS = Set.of("_meta", "profiles", "defaultProfile", "default_profile");

    private final SysThirdPartyProviderMapper providerMapper;
    private final SysThirdPartyRouteRuleMapper routeRuleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void initRuntimeCaches() {
        refreshOssRuntimeCache();
    }

    @Override
    public List<SysThirdPartyProvider> listProviders(String serviceType) {
        String cacheKey = CACHE_KEY_PROVIDER + serviceType;
        List<SysThirdPartyProvider> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 管理端需要能看到停用中的 provider，避免已存在配置却无法继续维护。
        List<SysThirdPartyProvider> providers = queryProviders(serviceType, true, false);
        RedisUtils.setCacheObject(cacheKey, providers, CACHE_TTL);
        return providers;
    }

    @Override
    public SysThirdPartyProvider getCurrentProvider(String serviceType) {
        if ("payment".equals(serviceType)) {
            return null;
        }

        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysThirdPartyProvider::getServiceType, serviceType);
        wrapper.eq(SysThirdPartyProvider::getIsCurrent, 1);
        wrapper.eq(SysThirdPartyProvider::getIsEnabled, 1);
        wrapper.ne(SysThirdPartyProvider::getProviderCode, COMMON_PROVIDER_CODE);
        wrapper.orderByAsc(SysThirdPartyProvider::getDisplayOrder, SysThirdPartyProvider::getProviderId);
        wrapper.last("LIMIT 1");
        return providerMapper.selectOne(wrapper);
    }

    @Override
    public SysThirdPartyProvider getProvider(String serviceType, String providerCode) {
        if (org.aileme.common.core.utils.StringUtils.isAnyBlank(serviceType, providerCode)) {
            return null;
        }
        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysThirdPartyProvider::getServiceType, serviceType);
        wrapper.eq(SysThirdPartyProvider::getProviderCode, providerCode);
        wrapper.orderByAsc(SysThirdPartyProvider::getProviderId);
        wrapper.last("LIMIT 1");
        return providerMapper.selectOne(wrapper);
    }

    @Override
    @DSTransactional
    public boolean switchProvider(String serviceType, String providerCode) {
        if (COMMON_PROVIDER_CODE.equalsIgnoreCase(providerCode)) {
            throw new RuntimeException("共享配置不允许切换为当前提供商");
        }

        if ("payment".equals(serviceType)) {
            SysThirdPartyProvider provider = getProvider(serviceType, providerCode);
            if (provider == null) {
                return false;
            }
            provider.setIsEnabled(provider.getIsEnabled() != null && provider.getIsEnabled() == 1 ? 0 : 1);
            boolean success = providerMapper.updateById(provider) > 0;
            if (success) {
                clearCache(serviceType);
            }
            return success;
        }

        LambdaUpdateWrapper<SysThirdPartyProvider> resetWrapper = new LambdaUpdateWrapper<>();
        resetWrapper.eq(SysThirdPartyProvider::getServiceType, serviceType);
        resetWrapper.ne(SysThirdPartyProvider::getProviderCode, COMMON_PROVIDER_CODE);
        resetWrapper.set(SysThirdPartyProvider::getIsCurrent, 0);
        providerMapper.update(null, resetWrapper);

        LambdaUpdateWrapper<SysThirdPartyProvider> targetWrapper = new LambdaUpdateWrapper<>();
        targetWrapper.eq(SysThirdPartyProvider::getServiceType, serviceType);
        targetWrapper.eq(SysThirdPartyProvider::getProviderCode, providerCode);
        targetWrapper.set(SysThirdPartyProvider::getIsCurrent, 1);
        targetWrapper.set(SysThirdPartyProvider::getIsEnabled, 1);
        boolean success = providerMapper.update(null, targetWrapper) > 0;
        if (success) {
            clearCache(serviceType);
        }
        return success;
    }

    @Override
    public List<SysThirdPartyConfigItem> listConfigs(String serviceType, String provider) {
        String cacheKey = CACHE_KEY_PREFIX + serviceType + ":" + provider;
        List<SysThirdPartyConfigItem> cached = RedisUtils.getCacheObject(cacheKey);
        if (cached != null) {
            return cached;
        }

        SysThirdPartyProvider providerEntity = getProvider(serviceType, provider);
        List<SysThirdPartyConfigItem> configs = buildConfigItems(providerEntity);
        RedisUtils.setCacheObject(cacheKey, configs, CACHE_TTL);
        return configs;
    }

    @Override
    public Map<String, List<SysThirdPartyConfigItem>> listAllConfigs(String serviceType) {
        List<SysThirdPartyProvider> providers = queryProviders(serviceType, false, true);
        Map<String, List<SysThirdPartyConfigItem>> result = new LinkedHashMap<>();
        for (SysThirdPartyProvider provider : providers) {
            result.put(provider.getProviderCode(), buildConfigItems(provider));
        }
        return result;
    }

    @Override
    public String getConfigValue(String serviceType, String provider, String configKey) {
        SysThirdPartyProvider providerEntity = getProvider(serviceType, provider);
        String value = readProviderConfigValue(providerEntity, configKey);
        if (org.aileme.common.core.utils.StringUtils.isNotBlank(value)) {
            return value;
        }
        if (!COMMON_PROVIDER_CODE.equalsIgnoreCase(provider)) {
            return readProviderConfigValue(getProvider(serviceType, COMMON_PROVIDER_CODE), configKey);
        }
        return null;
    }

    @Override
    public String getConfigValue(String serviceType, String configKey) {
        SysThirdPartyProvider currentProvider = getCurrentProvider(serviceType);
        if (currentProvider != null) {
            String value = getConfigValue(serviceType, currentProvider.getProviderCode(), configKey);
            if (org.aileme.common.core.utils.StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return getConfigValue(serviceType, COMMON_PROVIDER_CODE, configKey);
    }

    @Override
    @DSTransactional
    public boolean updateConfig(Long configId, String configValue) {
        ConfigRef ref = findConfigRef(configId);
        if (ref == null) {
            return false;
        }

        SysThirdPartyConfigItem config = ref.config();
        config.setConfigValue(configValue);
        updateProviderConfig(ref.provider(), config);
        clearCache(ref.provider().getServiceType());
        return true;
    }

    @Override
    @DSTransactional
    public boolean updateConfigBatch(List<SysThirdPartyConfigItem> configs) {
        if (configs == null || configs.isEmpty()) {
            return false;
        }

        Set<String> touchedServiceTypes = new HashSet<>();
        Map<String, List<SysThirdPartyConfigItem>> grouped = configs.stream()
            .map(this::normalizeConfigReference)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(item -> item.getServiceType() + "#" + item.getProvider(), LinkedHashMap::new, Collectors.toList()));

        for (List<SysThirdPartyConfigItem> providerConfigs : grouped.values()) {
            SysThirdPartyConfigItem first = providerConfigs.get(0);
            SysThirdPartyProvider provider = requireProvider(first.getServiceType(), first.getProvider());
            for (SysThirdPartyConfigItem config : providerConfigs) {
                updateProviderConfig(provider, config);
            }
            providerMapper.updateById(provider);
            touchedServiceTypes.add(first.getServiceType());
        }

        touchedServiceTypes.forEach(this::clearCache);
        return true;
    }

    @Override
    public String testConnection(String serviceType, String provider, Map<String, String> configs) {
        return switch (serviceType) {
            case "ai" -> testAIConnection(provider, configs);
            case "video" -> testVideoConnection(provider, configs);
            case "oss" -> testOSSConnection(provider, configs);
            case "sms" -> testSMSConnection(provider, configs);
            case "payment" -> testPaymentConnection(provider, configs);
            case "wecom_customer" -> testWecomConnection(configs);
            default -> "未知服务类型";
        };
    }

    private String testWecomConnection(Map<String, String> configs) {
        String corpId = configs.get("corpId");
        String corpSecret = configs.get("corpSecret");
        if (org.aileme.common.core.utils.StringUtils.isAnyBlank(corpId, corpSecret)) {
            return "缺少 corpId 或 corpSecret";
        }
        try {
            String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + corpSecret;
            String body = HttpUtil.get(url);
            Dict response = JsonUtils.parseMap(body);
            if (response == null) {
                return "企业微信响应为空";
            }
            Integer errCode = response.getInt("errcode");
            if (errCode == null) {
                errCode = -1;
            }
            if (Objects.equals(errCode, 0)) {
                return "企业微信连接成功";
            }
            String errMsg = response.getStr("errmsg");
            return "企业微信连接失败：" + (errMsg == null ? "unknown" : errMsg) + "（" + errCode + "）";
        } catch (Exception e) {
            return "企业微信连接失败：" + e.getMessage();
        }
    }

    @Override
    public SysThirdPartyConfigItem getConfigById(Long configId, boolean showSensitive) {
        ConfigRef ref = findConfigRef(configId);
        if (ref == null) {
            return null;
        }
        SysThirdPartyConfigItem config = ref.config();
        if (Objects.equals(config.getIsSensitive(), 1) && !showSensitive) {
            config.setConfigValue(maskSensitiveValue(config.getConfigValue()));
        }
        return config;
    }

    @Override
    public List<String> listServiceTypes() {
        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysThirdPartyProvider::getServiceType);
        wrapper.orderByAsc(SysThirdPartyProvider::getServiceType);
        return providerMapper.selectList(wrapper).stream()
            .map(SysThirdPartyProvider::getServiceType)
            .filter(org.aileme.common.core.utils.StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @DSTransactional
    public boolean addConfig(SysThirdPartyConfigItem config) {
        SysThirdPartyConfigItem normalized = normalizeConfigReference(config);
        if (normalized == null) {
            return false;
        }
        SysThirdPartyProvider provider = requireProvider(normalized.getServiceType(), normalized.getProvider());
        if (hasConfigKey(provider, normalized.getConfigKey())) {
            throw new RuntimeException("配置键已存在：" + normalized.getConfigKey());
        }
        updateProviderConfig(provider, normalized);
        providerMapper.updateById(provider);
        clearCache(normalized.getServiceType());
        return true;
    }

    @Override
    @DSTransactional
    public boolean deleteConfig(Long configId) {
        ConfigRef ref = findConfigRef(configId);
        if (ref == null) {
            return false;
        }
        removeProviderConfig(ref.provider(), ref.config().getConfigKey());
        providerMapper.updateById(ref.provider());
        clearCache(ref.provider().getServiceType());
        return true;
    }

    @Override
    @DSTransactional
    public boolean addProvider(SysThirdPartyProvider provider) {
        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysThirdPartyProvider::getServiceType, provider.getServiceType());
        wrapper.eq(SysThirdPartyProvider::getProviderCode, provider.getProviderCode());
        if (providerMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("服务提供商已存在：" + provider.getProviderCode());
        }

        if (provider.getIsCurrent() == null) {
            provider.setIsCurrent(0);
        }
        if (provider.getIsEnabled() == null) {
            provider.setIsEnabled(1);
        }
        if (provider.getDisplayOrder() == null) {
            LambdaQueryWrapper<SysThirdPartyProvider> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(SysThirdPartyProvider::getServiceType, provider.getServiceType());
            long count = providerMapper.selectCount(countWrapper);
            provider.setDisplayOrder((int) (count + 1));
        }
        if (org.aileme.common.core.utils.StringUtils.isBlank(provider.getConfigJson())) {
            provider.setConfigJson("{}");
        }

        boolean success = providerMapper.insert(provider) > 0;
        if (success) {
            clearCache(provider.getServiceType());
        }
        return success;
    }

    @Override
    @DSTransactional
    public boolean updateProvider(SysThirdPartyProvider provider) {
        if (provider == null || provider.getProviderId() == null) {
            return false;
        }
        SysThirdPartyProvider existing = providerMapper.selectById(provider.getProviderId());
        if (existing == null) {
            return false;
        }

        if (org.aileme.common.core.utils.StringUtils.isNotBlank(provider.getProviderCode())
            && !org.aileme.common.core.utils.StringUtils.equals(existing.getProviderCode(), provider.getProviderCode())) {
            LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysThirdPartyProvider::getServiceType, existing.getServiceType());
            wrapper.eq(SysThirdPartyProvider::getProviderCode, provider.getProviderCode());
            wrapper.ne(SysThirdPartyProvider::getProviderId, provider.getProviderId());
            if (providerMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("服务提供商编码已存在：" + provider.getProviderCode());
            }
        }

        existing.setProviderCode(org.aileme.common.core.utils.StringUtils.defaultIfBlank(provider.getProviderCode(), existing.getProviderCode()));
        existing.setProviderName(org.aileme.common.core.utils.StringUtils.defaultIfBlank(provider.getProviderName(), existing.getProviderName()));
        existing.setProviderLogo(provider.getProviderLogo());
        existing.setOfficialWebsite(provider.getOfficialWebsite());
        existing.setDocUrl(provider.getDocUrl());
        existing.setRemark(provider.getRemark());
        if (provider.getIsEnabled() != null) {
            existing.setIsEnabled(provider.getIsEnabled());
        }
        if (provider.getIsCurrent() != null) {
            existing.setIsCurrent(provider.getIsCurrent());
        }
        if (provider.getDisplayOrder() != null) {
            existing.setDisplayOrder(provider.getDisplayOrder());
        }
        if (org.aileme.common.core.utils.StringUtils.isNotBlank(provider.getConfigJson())) {
            existing.setConfigJson(provider.getConfigJson());
        }

        if (!"payment".equals(existing.getServiceType()) && Objects.equals(existing.getIsCurrent(), 1)) {
            LambdaUpdateWrapper<SysThirdPartyProvider> resetWrapper = new LambdaUpdateWrapper<>();
            resetWrapper.eq(SysThirdPartyProvider::getServiceType, existing.getServiceType());
            resetWrapper.ne(SysThirdPartyProvider::getProviderId, existing.getProviderId());
            resetWrapper.ne(SysThirdPartyProvider::getProviderCode, COMMON_PROVIDER_CODE);
            resetWrapper.set(SysThirdPartyProvider::getIsCurrent, 0);
            providerMapper.update(null, resetWrapper);
        }

        boolean success = providerMapper.updateById(existing) > 0;
        if (success) {
            clearCache(existing.getServiceType());
        }
        return success;
    }

    @Override
    @DSTransactional
    public boolean deleteProvider(Long providerId) {
        if (providerId == null) {
            return false;
        }
        SysThirdPartyProvider existing = providerMapper.selectById(providerId);
        if (existing == null) {
            return false;
        }
        if (COMMON_PROVIDER_CODE.equalsIgnoreCase(existing.getProviderCode())) {
            throw new RuntimeException("共享配置不允许删除");
        }

        LambdaQueryWrapper<SysThirdPartyRouteRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(SysThirdPartyRouteRule::getServiceType, existing.getServiceType());
        ruleWrapper.eq(SysThirdPartyRouteRule::getProviderCode, existing.getProviderCode());
        routeRuleMapper.delete(ruleWrapper);

        boolean success = providerMapper.deleteById(providerId) > 0;
        if (!success) {
            return false;
        }

        if (!"payment".equals(existing.getServiceType()) && Objects.equals(existing.getIsCurrent(), 1)) {
            LambdaQueryWrapper<SysThirdPartyProvider> nextWrapper = new LambdaQueryWrapper<>();
            nextWrapper.eq(SysThirdPartyProvider::getServiceType, existing.getServiceType());
            nextWrapper.eq(SysThirdPartyProvider::getIsEnabled, 1);
            nextWrapper.ne(SysThirdPartyProvider::getProviderCode, COMMON_PROVIDER_CODE);
            nextWrapper.orderByAsc(SysThirdPartyProvider::getDisplayOrder, SysThirdPartyProvider::getProviderId);
            nextWrapper.last("LIMIT 1");
            SysThirdPartyProvider next = providerMapper.selectOne(nextWrapper);
            if (next != null) {
                next.setIsCurrent(1);
                providerMapper.updateById(next);
            }
        }

        clearCache(existing.getServiceType());
        return true;
    }

    @Override
    @DSTransactional
    public boolean addConfigBatch(List<SysThirdPartyConfigItem> configs) {
        if (configs == null || configs.isEmpty()) {
            return true;
        }
        return updateConfigBatch(configs);
    }

    @Override
    public List<SysThirdPartyRouteRule> listRouteRules(String serviceType) {
        LambdaQueryWrapper<SysThirdPartyRouteRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(org.aileme.common.core.utils.StringUtils.isNotBlank(serviceType), SysThirdPartyRouteRule::getServiceType, serviceType);
        wrapper.orderByAsc(SysThirdPartyRouteRule::getServiceType)
            .orderByAsc(SysThirdPartyRouteRule::getSceneCode)
            .orderByAsc(SysThirdPartyRouteRule::getTemplateCode)
            .orderByAsc(SysThirdPartyRouteRule::getContentMode)
            .orderByAsc(SysThirdPartyRouteRule::getFunctionType)
            .orderByAsc(SysThirdPartyRouteRule::getPriority)
            .orderByAsc(SysThirdPartyRouteRule::getRouteRuleId);
        return routeRuleMapper.selectList(wrapper);
    }

    @Override
    @DSTransactional
    public boolean addRouteRule(SysThirdPartyRouteRule routeRule) {
        normalizeRouteRule(routeRule);
        return routeRuleMapper.insert(routeRule) > 0;
    }

    @Override
    @DSTransactional
    public boolean updateRouteRule(SysThirdPartyRouteRule routeRule) {
        if (routeRule == null || routeRule.getRouteRuleId() == null) {
            return false;
        }
        SysThirdPartyRouteRule existing = routeRuleMapper.selectById(routeRule.getRouteRuleId());
        if (existing == null) {
            return false;
        }
        normalizeRouteRule(routeRule);
        return routeRuleMapper.updateById(routeRule) > 0;
    }

    @Override
    @DSTransactional
    public boolean deleteRouteRule(Long routeRuleId) {
        if (routeRuleId == null) {
            return false;
        }
        return routeRuleMapper.deleteById(routeRuleId) > 0;
    }

    private List<SysThirdPartyProvider> queryProviders(String serviceType, boolean excludeCommon, boolean enabledOnly) {
        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysThirdPartyProvider::getServiceType, serviceType);
        if (enabledOnly) {
            wrapper.eq(SysThirdPartyProvider::getIsEnabled, 1);
        }
        if (excludeCommon) {
            wrapper.ne(SysThirdPartyProvider::getProviderCode, COMMON_PROVIDER_CODE);
        }
        wrapper.orderByAsc(SysThirdPartyProvider::getDisplayOrder, SysThirdPartyProvider::getProviderId);
        return providerMapper.selectList(wrapper);
    }

    private void normalizeRouteRule(SysThirdPartyRouteRule routeRule) {
        if (routeRule == null) {
            throw new RuntimeException("路由规则不能为空");
        }
        routeRule.setServiceType(org.aileme.common.core.utils.StringUtils.trimToEmpty(routeRule.getServiceType()));
        routeRule.setSceneCode(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getSceneCode()));
        routeRule.setTemplateCode(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getTemplateCode()));
        routeRule.setContentMode(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getContentMode()));
        routeRule.setFunctionType(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getFunctionType()));
        routeRule.setProviderCode(org.aileme.common.core.utils.StringUtils.trimToEmpty(routeRule.getProviderCode()));
        routeRule.setProfileCode(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getProfileCode()));
        routeRule.setMatchJson(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getMatchJson()));
        routeRule.setRemark(org.aileme.common.core.utils.StringUtils.trimToNull(routeRule.getRemark()));
        routeRule.setPriority(routeRule.getPriority() == null ? 100 : routeRule.getPriority());
        routeRule.setIsEnabled(routeRule.getIsEnabled() == null ? 1 : routeRule.getIsEnabled());

        if (org.aileme.common.core.utils.StringUtils.isBlank(routeRule.getServiceType())) {
            throw new RuntimeException("服务类型不能为空");
        }
        if (org.aileme.common.core.utils.StringUtils.isBlank(routeRule.getProviderCode())) {
            throw new RuntimeException("服务商编码不能为空");
        }
        if (org.aileme.common.core.utils.StringUtils.isNotBlank(routeRule.getMatchJson()) && !JsonUtils.isJson(routeRule.getMatchJson())) {
            throw new RuntimeException("matchJson 必须是合法 JSON");
        }
        requireProvider(routeRule.getServiceType(), routeRule.getProviderCode());
    }

    private SysThirdPartyProvider requireProvider(String serviceType, String providerCode) {
        SysThirdPartyProvider provider = getProvider(serviceType, providerCode);
        if (provider == null) {
            throw new RuntimeException("未找到提供商配置: " + serviceType + "/" + providerCode);
        }
        if (org.aileme.common.core.utils.StringUtils.isBlank(provider.getConfigJson())) {
            provider.setConfigJson("{}");
        }
        return provider;
    }

    private List<SysThirdPartyConfigItem> buildConfigItems(SysThirdPartyProvider provider) {
        if (provider == null) {
            return Collections.emptyList();
        }
        ObjectNode root = readProviderConfigObject(provider);
        ObjectNode metaNode = root.path("_meta").isObject() ? (ObjectNode) root.get("_meta") : objectMapper.createObjectNode();
        LinkedHashSet<String> keys = new LinkedHashSet<>();

        metaNode.fieldNames().forEachRemaining(keys::add);
        root.fields().forEachRemaining(entry -> {
            if (RESERVED_CONFIG_KEYS.contains(entry.getKey())) {
                return;
            }
            if (!entry.getValue().isContainerNode()) {
                keys.add(entry.getKey());
            }
        });

        List<String> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(Comparator
            .comparingInt((String key) -> readMetaInt(metaNode.get(key), "displayOrder", Integer.MAX_VALUE / 2))
            .thenComparing(String::compareTo));

        List<SysThirdPartyConfigItem> configs = new ArrayList<>();
        for (String key : sortedKeys) {
            SysThirdPartyConfigItem config = new SysThirdPartyConfigItem();
            config.setConfigId(buildSyntheticConfigId(provider.getServiceType(), provider.getProviderCode(), key));
            config.setServiceType(provider.getServiceType());
            config.setProvider(provider.getProviderCode());
            config.setConfigKey(key);
            config.setConfigValue(readNodeText(root.get(key)));
            config.setDefaultValue(readMetaText(metaNode.get(key), "defaultValue", "default_value"));
            config.setValueType(resolveValueType(root.get(key), metaNode.get(key), key));
            config.setSelectOptions(readMetaText(metaNode.get(key), "selectOptions", "select_options"));
            config.setIsRequired(readMetaInt(metaNode.get(key), "isRequired", 0));
            config.setIsSensitive(readMetaInt(metaNode.get(key), "isSensitive", 0));
            config.setIsEnabled(1);
            config.setDisplayOrder(readMetaInt(metaNode.get(key), "displayOrder", configs.size() + 1));
            config.setConfigLabel(readMetaText(metaNode.get(key), "configLabel", "config_label"));
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getConfigLabel())) {
                config.setConfigLabel(buildDisplayLabel(key));
            }
            config.setHelpText(readMetaText(metaNode.get(key), "helpText", "help_text"));
            configs.add(config);
        }
        return configs;
    }

    private String readProviderConfigValue(SysThirdPartyProvider provider, String configKey) {
        if (provider == null || org.aileme.common.core.utils.StringUtils.isBlank(configKey)) {
            return null;
        }
        ObjectNode root = readProviderConfigObject(provider);
        JsonNode direct = root.get(configKey);
        if (direct != null && !direct.isContainerNode() && org.aileme.common.core.utils.StringUtils.isNotBlank(readNodeText(direct))) {
            return readNodeText(direct);
        }
        JsonNode relaxed = root.get(toSnakeCase(configKey));
        if (relaxed != null && !relaxed.isContainerNode() && org.aileme.common.core.utils.StringUtils.isNotBlank(readNodeText(relaxed))) {
            return readNodeText(relaxed);
        }
        JsonNode profiles = root.get("profiles");
        String defaultProfile = readNodeText(root.get("defaultProfile"));
        if (org.aileme.common.core.utils.StringUtils.isBlank(defaultProfile)) {
            defaultProfile = readNodeText(root.get("default_profile"));
        }
        if (profiles != null && profiles.isArray()) {
            for (JsonNode profile : profiles) {
                if (!profile.isObject()) {
                    continue;
                }
                String profileCode = readNodeText(profile.get("code"));
                if (org.aileme.common.core.utils.StringUtils.isBlank(profileCode)) {
                    profileCode = readNodeText(profile.get("profileCode"));
                }
                if (org.aileme.common.core.utils.StringUtils.isBlank(profileCode)) {
                    profileCode = readNodeText(profile.get("profile_code"));
                }
                if (org.aileme.common.core.utils.StringUtils.isNotBlank(defaultProfile)
                    && org.aileme.common.core.utils.StringUtils.isNotBlank(profileCode)
                    && !org.aileme.common.core.utils.StringUtils.equalsIgnoreCase(defaultProfile, profileCode)) {
                    continue;
                }
                JsonNode profileValue = profile.get(configKey);
                if (profileValue == null) {
                    profileValue = profile.get(toSnakeCase(configKey));
                }
                if (profileValue != null && !profileValue.isContainerNode()) {
                    return readNodeText(profileValue);
                }
            }
        }
        return null;
    }

    private void updateProviderConfig(SysThirdPartyProvider provider, SysThirdPartyConfigItem config) {
        ObjectNode root = readProviderConfigObject(provider);
        root.set(config.getConfigKey(), toValueNode(config.getConfigValue(), config.getValueType()));

        ObjectNode metaNode = root.path("_meta").isObject() ? (ObjectNode) root.get("_meta") : objectMapper.createObjectNode();
        ObjectNode keyMeta = metaNode.path(config.getConfigKey()).isObject() ? (ObjectNode) metaNode.get(config.getConfigKey()) : objectMapper.createObjectNode();
        keyMeta.put("configLabel", org.aileme.common.core.utils.StringUtils.defaultIfBlank(config.getConfigLabel(), buildDisplayLabel(config.getConfigKey())));
        keyMeta.put("defaultValue", org.aileme.common.core.utils.StringUtils.defaultString(config.getDefaultValue()));
        keyMeta.put("valueType", org.aileme.common.core.utils.StringUtils.defaultIfBlank(config.getValueType(), "text"));
        keyMeta.put("selectOptions", org.aileme.common.core.utils.StringUtils.defaultString(config.getSelectOptions()));
        keyMeta.put("isRequired", config.getIsRequired() == null ? 0 : config.getIsRequired());
        keyMeta.put("isSensitive", config.getIsSensitive() == null ? 0 : config.getIsSensitive());
        keyMeta.put("displayOrder", config.getDisplayOrder() == null ? Integer.MAX_VALUE / 2 : config.getDisplayOrder());
        keyMeta.put("helpText", org.aileme.common.core.utils.StringUtils.defaultString(config.getHelpText()));
        metaNode.set(config.getConfigKey(), keyMeta);
        cleanupLegacyOssConfigKeys(provider, root, metaNode, config.getConfigKey());
        root.set("_meta", metaNode);
        provider.setConfigJson(writeJson(root));
    }

    private void cleanupLegacyOssConfigKeys(SysThirdPartyProvider provider, ObjectNode root, ObjectNode metaNode, String configKey) {
        if (!"oss".equalsIgnoreCase(provider.getServiceType())) {
            return;
        }
        if ("bucket".equals(configKey)) {
            root.remove("bucket_name");
            root.remove("bucketName");
            metaNode.remove("bucket_name");
            metaNode.remove("bucketName");
            return;
        }
        if ("access_policy".equals(configKey)) {
            root.remove("accessPolicy");
            metaNode.remove("accessPolicy");
        }
    }

    private void removeProviderConfig(SysThirdPartyProvider provider, String configKey) {
        ObjectNode root = readProviderConfigObject(provider);
        root.remove(configKey);
        if (root.path("_meta").isObject()) {
            ((ObjectNode) root.get("_meta")).remove(configKey);
        }
        provider.setConfigJson(writeJson(root));
    }

    private boolean hasConfigKey(SysThirdPartyProvider provider, String configKey) {
        ObjectNode root = readProviderConfigObject(provider);
        return root.has(configKey) || (root.path("_meta").isObject() && root.path("_meta").has(configKey));
    }

    private ConfigRef findConfigRef(Long configId) {
        if (configId == null) {
            return null;
        }
        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysThirdPartyProvider::getIsEnabled, 1);
        wrapper.orderByAsc(SysThirdPartyProvider::getServiceType, SysThirdPartyProvider::getDisplayOrder, SysThirdPartyProvider::getProviderId);
        List<SysThirdPartyProvider> providers = providerMapper.selectList(wrapper);
        for (SysThirdPartyProvider provider : providers) {
            for (SysThirdPartyConfigItem config : buildConfigItems(provider)) {
                if (Objects.equals(config.getConfigId(), configId)) {
                    return new ConfigRef(provider, config);
                }
            }
        }
        return null;
    }

    private SysThirdPartyConfigItem normalizeConfigReference(SysThirdPartyConfigItem source) {
        if (source == null) {
            return null;
        }
        SysThirdPartyConfigItem config = source;
        if (org.aileme.common.core.utils.StringUtils.isBlank(config.getProvider()) || org.aileme.common.core.utils.StringUtils.isBlank(config.getServiceType())) {
            ConfigRef ref = findConfigRef(config.getConfigId());
            if (ref == null) {
                return null;
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getServiceType())) {
                config.setServiceType(ref.provider().getServiceType());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getProvider())) {
                config.setProvider(ref.provider().getProviderCode());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getConfigKey())) {
                config.setConfigKey(ref.config().getConfigKey());
            }
            if (config.getDisplayOrder() == null) {
                config.setDisplayOrder(ref.config().getDisplayOrder());
            }
            if (config.getIsRequired() == null) {
                config.setIsRequired(ref.config().getIsRequired());
            }
            if (config.getIsSensitive() == null) {
                config.setIsSensitive(ref.config().getIsSensitive());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getConfigLabel())) {
                config.setConfigLabel(ref.config().getConfigLabel());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getValueType())) {
                config.setValueType(ref.config().getValueType());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getSelectOptions())) {
                config.setSelectOptions(ref.config().getSelectOptions());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getHelpText())) {
                config.setHelpText(ref.config().getHelpText());
            }
            if (org.aileme.common.core.utils.StringUtils.isBlank(config.getDefaultValue())) {
                config.setDefaultValue(ref.config().getDefaultValue());
            }
        }
        return config;
    }

    private void clearCache(String serviceType) {
        RedisUtils.deleteObject(CACHE_KEY_PROVIDER + serviceType);
        for (SysThirdPartyProvider provider : queryProviders(serviceType, false, true)) {
            RedisUtils.deleteObject(CACHE_KEY_PREFIX + serviceType + ":" + provider.getProviderCode());
        }

        if ("oss".equalsIgnoreCase(serviceType)) {
            refreshOssRuntimeCache();
        }
    }

    private void refreshOssRuntimeCache() {
        LambdaQueryWrapper<SysThirdPartyProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysThirdPartyProvider::getServiceType, "oss");
        wrapper.eq(SysThirdPartyProvider::getIsEnabled, 1);
        wrapper.ne(SysThirdPartyProvider::getProviderCode, COMMON_PROVIDER_CODE);
        wrapper.orderByAsc(SysThirdPartyProvider::getDisplayOrder, SysThirdPartyProvider::getProviderId);
        List<SysThirdPartyProvider> providers = providerMapper.selectList(wrapper);

        RedisUtils.deleteObject(OssConstant.DEFAULT_CONFIG_KEY);
        for (SysThirdPartyProvider provider : providers) {
            CacheUtils.evict(CacheNames.SYS_THIRD_OSS_CONFIG, provider.getProviderCode());
        }

        if (providers.isEmpty()) {
            log.warn("未找到启用的 OSS 三方配置，文件上传运行时缓存未初始化");
            return;
        }

        SysThirdPartyProvider currentProvider = providers.stream()
            .filter(provider -> provider.getIsCurrent() != null && provider.getIsCurrent() == 1)
            .findFirst()
            .orElse(providers.get(0));
        RedisUtils.setCacheObject(OssConstant.DEFAULT_CONFIG_KEY, currentProvider.getProviderCode());

        for (SysThirdPartyProvider provider : providers) {
            OssProperties ossProperties = buildOssProperties(provider);
            CacheUtils.put(CacheNames.SYS_THIRD_OSS_CONFIG, provider.getProviderCode(), JsonUtils.toJsonString(ossProperties));
        }
    }

    private OssProperties buildOssProperties(SysThirdPartyProvider provider) {
        Dict jsonConfig = JsonUtils.parseMap(provider.getConfigJson());
        OssProperties properties = new OssProperties();
        properties.setTenantId(readJsonConfig(jsonConfig, "tenantId", "tenant_id"));
        properties.setEndpoint(readJsonConfig(jsonConfig, "endpoint"));
        properties.setDomain(readJsonConfig(jsonConfig, "domain"));
        properties.setPrefix(readJsonConfig(jsonConfig, "prefix"));
        properties.setAccessKey(readJsonConfig(jsonConfig, "accessKey", "access_key", "secret_id", "access_key_id"));
        properties.setSecretKey(readJsonConfig(jsonConfig, "secretKey", "secret_key", "secret", "access_key_secret"));
        properties.setBucketName(readJsonConfig(jsonConfig, "bucketName", "bucket", "bucket_name"));
        properties.setRegion(readJsonConfig(jsonConfig, "region"));
        properties.setIsHttps(normalizeHttpsFlag(readJsonConfig(jsonConfig, "isHttps", "is_https")));
        properties.setAccessPolicy(normalizeAccessPolicy(readJsonConfig(jsonConfig, "accessPolicy", "access_policy")));
        return properties;
    }

    private String readJsonConfig(Dict jsonConfig, String... keys) {
        if (jsonConfig == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = jsonConfig.getStr(key);
            if (org.aileme.common.core.utils.StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeHttpsFlag(String value) {
        if (org.aileme.common.core.utils.StringUtils.isBlank(value)) {
            return OssConstant.IS_HTTPS;
        }
        if ("1".equals(value) || "true".equalsIgnoreCase(value) || "y".equalsIgnoreCase(value)) {
            return OssConstant.IS_HTTPS;
        }
        return "N";
    }

    private String normalizeAccessPolicy(String value) {
        return org.aileme.common.core.utils.StringUtils.isBlank(value) ? AccessPolicyType.PUBLIC.getType() : value;
    }

    private String maskSensitiveValue(String value) {
        if (org.aileme.common.core.utils.StringUtils.isBlank(value) || value.length() < 8) {
            return "******";
        }
        return value.substring(0, 4) + "******" + value.substring(value.length() - 4);
    }

    private String testAIConnection(String provider, Map<String, String> configs) {
        String apiKey = configs.get("api_key");
        if (org.aileme.common.core.utils.StringUtils.isBlank(apiKey)) {
            return "API Key不能为空";
        }
        return switch (provider) {
            case "tongyi" -> "通义千问连接测试成功(模拟)";
            case "zhipu" -> "智谱GLM连接测试成功(模拟)";
            default -> "未知的AI提供商";
        };
    }

    private String testVideoConnection(String provider, Map<String, String> configs) {
        if ("jimeng".equals(provider)) {
            String accessKeyId = configs.get("access_key_id");
            String apiKey = configs.get("api_key");
            if (org.aileme.common.core.utils.StringUtils.isBlank(accessKeyId)
                && org.aileme.common.core.utils.StringUtils.isBlank(apiKey)) {
                return "即梦配置不完整：请至少配置 access_key_id 或 api_key";
            }
            return "视频服务连接测试通过(模拟)";
        }

        String apiKey = configs.get("api_key");
        if (org.aileme.common.core.utils.StringUtils.isBlank(apiKey)) {
            return "视频服务API Key不能为空";
        }
        return "视频服务连接测试通过(模拟)";
    }

    private String testOSSConnection(String provider, Map<String, String> configs) {
        if ("aliyun".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("access_key_id"),
                    configs.get("access_key_secret"),
                    configs.get("bucket"),
                    configs.get("endpoint"))) {
                return "阿里云 OSS 配置不完整：需补齐 access_key_id/access_key_secret/bucket/endpoint";
            }
            return "阿里云 OSS 配置校验通过";
        }
        if ("tencent".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("secret_id"),
                    configs.get("secret_key"),
                    configs.get("bucket"),
                    configs.get("region"))) {
                return "腾讯云 COS 配置不完整：需补齐 secret_id/secret_key/bucket/region";
            }
            return "腾讯云 COS 配置校验通过";
        }
        if ("qiniu".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("access_key"),
                    configs.get("secret_key"),
                    configs.get("bucket"),
                    configs.get("endpoint"))) {
                return "七牛云配置不完整：需补齐 access_key/secret_key/bucket/endpoint";
            }
            return "七牛云配置校验通过";
        }
        return "云存储服务测试暂未支持该 provider";
    }

    private String testSMSConnection(String provider, Map<String, String> configs) {
        if ("aliyun".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("region_id"),
                    configs.get("access_key_id"),
                    configs.get("access_key_secret"),
                    configs.get("sign_name"),
                    configs.get("template_login"))) {
                return "阿里云短信配置不完整：需补齐 region_id/access_key_id/access_key_secret/sign_name/template_login";
            }
            return "阿里云短信配置校验通过";
        }
        if ("tencent".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("secret_id"),
                    configs.get("secret_key"),
                    configs.get("app_id"),
                    configs.get("sign_name"),
                    configs.get("template_login"))) {
                return "腾讯云短信配置不完整：需补齐 secret_id/secret_key/app_id/sign_name/template_login";
            }
            return "腾讯云短信配置校验通过，发送通路待接入";
        }
        return "短信服务测试暂未支持该 provider";
    }

    private String testPaymentConnection(String provider, Map<String, String> configs) {
        if ("wechat".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("mch_id"),
                    configs.get("api_key"),
                    configs.get("notify_url"))) {
                return "微信支付配置不完整：需补齐 mch_id/api_key/notify_url";
            }
            return "微信支付配置校验通过";
        }
        if ("alipay".equalsIgnoreCase(provider)) {
            if (org.aileme.common.core.utils.StringUtils.isAnyBlank(
                    configs.get("app_id"),
                    configs.get("private_key"),
                    configs.get("alipay_public_key"),
                    configs.get("notify_url"))) {
                return "支付宝配置不完整：需补齐 app_id/private_key/alipay_public_key/notify_url";
            }
            return "支付宝配置校验通过";
        }
        return "支付服务测试暂未支持该 provider";
    }

    private ObjectNode readProviderConfigObject(SysThirdPartyProvider provider) {
        if (provider == null || org.aileme.common.core.utils.StringUtils.isBlank(provider.getConfigJson())) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(provider.getConfigJson());
            return root != null && root.isObject() ? (ObjectNode) root : objectMapper.createObjectNode();
        } catch (Exception e) {
            log.warn("解析 provider.config_json 失败: {}/{}", provider.getServiceType(), provider.getProviderCode(), e);
            return objectMapper.createObjectNode();
        }
    }

    private String writeJson(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("写入 provider.config_json 失败", e);
        }
    }

    private JsonNode toValueNode(String value, String valueType) {
        String normalizedType = org.aileme.common.core.utils.StringUtils.defaultIfBlank(valueType, "text");
        try {
            if ("number".equalsIgnoreCase(normalizedType)) {
                if (org.aileme.common.core.utils.StringUtils.isBlank(value)) {
                    return objectMapper.getNodeFactory().numberNode(0);
                }
                if (value.contains(".")) {
                    return objectMapper.getNodeFactory().numberNode(Double.parseDouble(value));
                }
                return objectMapper.getNodeFactory().numberNode(Long.parseLong(value));
            }
            if ("boolean".equalsIgnoreCase(normalizedType)) {
                return objectMapper.getNodeFactory().booleanNode(Boolean.parseBoolean(org.aileme.common.core.utils.StringUtils.defaultString(value)));
            }
            if ("json".equalsIgnoreCase(normalizedType) && org.aileme.common.core.utils.StringUtils.isNotBlank(value) && JsonUtils.isJson(value)) {
                return objectMapper.readTree(value);
            }
        } catch (Exception ignored) {
            // fallback to string
        }
        return objectMapper.getNodeFactory().textNode(org.aileme.common.core.utils.StringUtils.defaultString(value));
    }

    private String resolveValueType(JsonNode valueNode, JsonNode metaNode, String configKey) {
        String metaValueType = readMetaText(metaNode, "valueType", "value_type");
        if (org.aileme.common.core.utils.StringUtils.isNotBlank(metaValueType)) {
            return metaValueType;
        }
        if (valueNode != null) {
            if (valueNode.isBoolean()) {
                return "boolean";
            }
            if (valueNode.isNumber()) {
                return "number";
            }
            if (valueNode.isContainerNode()) {
                return "json";
            }
        }
        String key = org.aileme.common.core.utils.StringUtils.defaultString(configKey).toLowerCase();
        if (key.contains("secret") || key.contains("token") || key.contains("private") || key.contains("password") || key.contains("api_key")) {
            return "password";
        }
        if (key.contains("timeout") || key.contains("cost") || key.contains("count") || key.contains("times") || key.contains("concurrency")) {
            return "number";
        }
        return "text";
    }

    private String buildDisplayLabel(String configKey) {
        String[] parts = org.aileme.common.core.utils.StringUtils.defaultString(configKey).split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (org.aileme.common.core.utils.StringUtils.isBlank(part)) {
                continue;
            }
            if ("api".equalsIgnoreCase(part) || "id".equalsIgnoreCase(part) || "url".equalsIgnoreCase(part)) {
                words.add(part.toUpperCase());
            } else {
                words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase());
            }
        }
        return words.isEmpty() ? configKey : String.join(" ", words);
    }

    private String readMetaText(JsonNode metaNode, String primaryKey, String secondaryKey) {
        if (metaNode == null || !metaNode.isObject()) {
            return null;
        }
        JsonNode direct = metaNode.get(primaryKey);
        if (direct == null) {
            direct = metaNode.get(secondaryKey);
        }
        return readNodeText(direct);
    }

    private int readMetaInt(JsonNode metaNode, String key, int defaultValue) {
        if (metaNode == null || !metaNode.isObject()) {
            return defaultValue;
        }
        JsonNode node = metaNode.get(key);
        if (node == null) {
            node = metaNode.get(toSnakeCase(key));
        }
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        if (node.isNumber()) {
            return node.asInt(defaultValue);
        }
        String value = node.asText("");
        if (org.aileme.common.core.utils.StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String readNodeText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return node.asText();
        }
    }

    private Long buildSyntheticConfigId(String serviceType, String providerCode, String configKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((serviceType + "#" + providerCode + "#" + configKey).getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest, 0, 8).getLong();
            return value == Long.MIN_VALUE ? 0L : Math.abs(value);
        } catch (Exception e) {
            throw new RuntimeException("生成配置ID失败", e);
        }
    }

    private String toSnakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private record ConfigRef(SysThirdPartyProvider provider, SysThirdPartyConfigItem config) {
    }
}
