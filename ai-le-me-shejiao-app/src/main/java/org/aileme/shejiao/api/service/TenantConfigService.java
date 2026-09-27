package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.TenantConfigEntity;

import java.util.List;
import java.util.Map;

/**
 * 租户配置Service
 *
 * @author system
 * @date 2026-01-30
 */
public interface TenantConfigService extends IService<TenantConfigEntity> {

    /**
     * 分页查询租户配置列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存租户配置
     */
    void saveTenantConfig(TenantConfigEntity config);

    /**
     * 更新租户配置
     */
    void updateTenantConfig(TenantConfigEntity config);

    /**
     * 根据租户ID和配置键获取配置
     */
    TenantConfigEntity getByTenantIdAndKey(Long tenantId, String configKey);

    /**
     * 获取租户的所有配置
     */
    Map<String, Object> getAllConfigsByTenantId(Long tenantId);

    /**
     * 获取租户配置值
     */
    <T> T getConfigValue(Long tenantId, String configKey, Class<T> type);

    /**
     * 根据租户ID获取配置列表
     */
    List<TenantConfigEntity> getConfigByTenantId(Long tenantId);

    /**
     * 批量保存租户配置
     */
    void batchSave(List<TenantConfigEntity> configs);
}