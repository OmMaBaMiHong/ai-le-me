package org.aileme.shejiao.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.oss.factory.OssFactory;
import org.aileme.system.domain.SysThirdPartyProvider;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 为用户资料中的图片地址补齐私有 OSS 访问签名。
 * 这里不依赖 accessPolicy 配置，避免线上桶已切私有但后台配置未同步时头像不可见。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssMediaUrlResolver {

    private static final Duration URL_EXPIRE = Duration.ofMinutes(10);

    private final ISysThirdPartyService thirdPartyService;

    public String resolve(String url) {
        if (StringUtils.isBlank(url) || isLocalOrSignedUrl(url)) {
            return url;
        }
        ProviderMatch match = matchProvider(url);
        if (match == null) {
            return url;
        }
        String objectKey = extractObjectKey(url);
        if (StringUtils.isBlank(objectKey)) {
            return url;
        }
        try {
            return OssFactory.instance(match.providerCode()).createPresignedGetUrl(objectKey, URL_EXPIRE);
        } catch (Exception e) {
            log.warn("生成私有OSS签名链接失败, provider={}, url={}", match.providerCode(), StringUtils.abbreviate(url, 160), e);
            return url;
        }
    }

    public List<String> resolveList(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> resolved = new ArrayList<>(urls.size());
        for (String url : urls) {
            resolved.add(resolve(url));
        }
        return resolved;
    }

    public void resolveAppUser(AppUserEntity user) {
        if (user == null) {
            return;
        }
        user.setAvatar(resolve(user.getAvatar()));
    }

    private ProviderMatch matchProvider(String url) {
        URI targetUri = parseUri(url);
        if (targetUri == null || StringUtils.isBlank(targetUri.getHost())) {
            return null;
        }
        List<SysThirdPartyProvider> providers = thirdPartyService.listProviders("oss");
        for (SysThirdPartyProvider provider : providers) {
            JSONObject config = parseConfig(provider.getConfigJson());
            if (config == null) {
                continue;
            }
            if (hostMatches(targetUri, readConfig(config, "domain", "domain"))
                || hostMatches(targetUri, readConfig(config, "endpoint", "endpoint"))) {
                return new ProviderMatch(provider.getProviderCode());
            }
        }
        return null;
    }

    private boolean isLocalOrSignedUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.startsWith("/") || lower.startsWith("data:")) {
            return true;
        }
        URI uri = parseUri(url);
        if (uri == null) {
            return false;
        }
        String query = StringUtils.defaultString(uri.getQuery()).toLowerCase();
        return query.contains("x-amz-algorithm=") || query.contains("token=");
    }

    private boolean hostMatches(URI targetUri, String candidate) {
        if (StringUtils.isBlank(candidate)) {
            return false;
        }
        URI candidateUri = parseUri(normalizeUrl(candidate));
        if (candidateUri == null || StringUtils.isBlank(candidateUri.getHost())) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(targetUri.getHost(), candidateUri.getHost());
    }

    private String extractObjectKey(String url) {
        URI uri = parseUri(url);
        if (uri == null || StringUtils.isBlank(uri.getPath())) {
            return null;
        }
        return StringUtils.removeStart(uri.getPath(), "/");
    }

    private JSONObject parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            return null;
        }
        try {
            return JSON.parseObject(configJson);
        } catch (Exception e) {
            log.warn("解析OSS配置失败, configJson={}", StringUtils.abbreviate(configJson, 160), e);
            return null;
        }
    }

    private String readConfig(JSONObject config, String... keys) {
        if (config == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = config.getString(key);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeUrl(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    private record ProviderMatch(String providerCode) {
    }
}
