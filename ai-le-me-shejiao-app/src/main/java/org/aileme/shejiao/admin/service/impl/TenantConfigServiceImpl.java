package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.TenantConfigDao;
import org.aileme.shejiao.api.service.TenantConfigService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.TenantConfigEntity;

import java.util.*;

/**
 * 租户配置Service实现
 *
 * @author system
 * @date 2026-01-30
 */
@DS("master")
@Service
public class TenantConfigServiceImpl extends ServiceImpl<TenantConfigDao, TenantConfigEntity> implements TenantConfigService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<TenantConfigEntity> page = this.page(
                new Query<TenantConfigEntity>().getPage(params),
                new QueryWrapper<TenantConfigEntity>()
                        .like(params.get("configKey") != null, "config_key", params.get("configKey"))
                        .eq(params.get("tenantId") != null, "tenant_id", params.get("tenantId"))
                        .orderByDesc("create_time")
        );

        return new PageUtils(page);
    }

    @Override
    public void saveTenantConfig(TenantConfigEntity config) {
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        this.save(config);
    }

    @Override
    public void updateTenantConfig(TenantConfigEntity config) {
        config.setUpdateTime(new Date());
        this.updateById(config);
    }

    @Override
    public TenantConfigEntity getByTenantIdAndKey(Long tenantId, String configKey) {
        return this.getOne(new QueryWrapper<TenantConfigEntity>()
                .eq("tenant_id", tenantId)
                .eq("config_key", configKey));
    }

    @Override
    public Map<String, Object> getAllConfigsByTenantId(Long tenantId) {
        List<TenantConfigEntity> configs = this.list(new QueryWrapper<TenantConfigEntity>()
                .eq("tenant_id", tenantId));

        Map<String, Object> result = new HashMap<>();
        for (TenantConfigEntity config : configs) {
            result.put(config.getConfigKey(), convertValue(config.getConfigValue(), config.getConfigType()));
        }
        return result;
    }

    @Override
    public <T> T getConfigValue(Long tenantId, String configKey, Class<T> type) {
        TenantConfigEntity config = getByTenantIdAndKey(tenantId, configKey);
        if (config != null) {
            Object value = convertValue(config.getConfigValue(), config.getConfigType());
            return type.cast(value);
        }
        return null;
    }

    @Override
    public List<TenantConfigEntity> getConfigByTenantId(Long tenantId) {
        return this.list(new QueryWrapper<TenantConfigEntity>()
                .eq("tenant_id", tenantId));
    }

    @Override
    public void batchSave(List<TenantConfigEntity> configs) {
        Date now = new Date();
        for (TenantConfigEntity config : configs) {
            if (config.getId() != null) {
                // 更新现有配置
                config.setUpdateTime(now);
                this.updateById(config);
            } else {
                // 新增配置
                config.setCreateTime(now);
                config.setUpdateTime(now);
                this.save(config);
            }
        }
    }

    /**
     * 根据配置类型转换值
     */
    private Object convertValue(String value, String type) {
        if (value == null) {
            return null;
        }

        switch (type.toUpperCase()) {
            case "NUMBER":
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException ex) {
                        return value;
                    }
                }
            case "BOOLEAN":
                return Boolean.parseBoolean(value);
            case "JSON":
                // 简单处理，实际项目中可能需要使用Jackson等工具
                return value;
            default:
                return value;
        }
    }
}
