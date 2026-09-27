package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.admin.TenantConfigEntity;
/**
 * 租户配置Dao
 *
 * @author system
 * @date 2026-01-30
 */
@Mapper
public interface TenantConfigDao extends BaseMapper<TenantConfigEntity> {
}