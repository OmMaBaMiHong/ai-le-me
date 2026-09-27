package org.aileme.shejiao.app.runtime.config;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.commons.lang3.StringUtils;
import org.aileme.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PlatformBusinessConfigService;

/**
 * 平台业务配置统一门面。
 */
@DS("master")
@Service
public class PlatformBusinessConfigServiceImpl implements PlatformBusinessConfigService {

    private final ISysConfigService sysConfigService;

    public PlatformBusinessConfigServiceImpl(ISysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Override
    public String getString(String key) {
        return sysConfigService.selectConfigByKey(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    @Override
    public Integer getInt(String key, Integer defaultValue) {
        String value = getString(key);
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
    public Long getLong(String key, Long defaultValue) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getString(key);
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
}
