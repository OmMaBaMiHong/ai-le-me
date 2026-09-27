package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;

/**
 * 用户画像快照DAO
 *
 * @author system
 * @date 2026-02-14
 */
@Mapper
public interface UserPersonaSnapshotDao extends BaseMapper<UserPersonaSnapshotEntity> {
}
