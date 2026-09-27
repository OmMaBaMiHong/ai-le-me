package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;

/**
 * 用户画像快照服务接口
 *
 * @author system
 * @date 2026-02-14
 */
public interface UserPersonaSnapshotService extends IService<UserPersonaSnapshotEntity> {

    /**
     * 获取用户最新画像
     *
     * @param userId 用户ID
     * @return 画像快照
     */
    UserPersonaSnapshotEntity getLatestByUserId(Integer userId);

    /**
     * 生成用户画像
     *
     * @param userId 用户ID
     * @param source 生成来源
     * @return 画像快照
     */
    UserPersonaSnapshotEntity generatePersona(Integer userId, String source);
}
