package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.PersonaViewRecordEntity;

/**
 * 画像查看记录服务接口
 *
 * @author system
 * @date 2026-02-14
 */
public interface PersonaViewRecordService extends IService<PersonaViewRecordEntity> {

    /**
     * 检查是否已解锁
     *
     * @param viewerId     查看者ID
     * @param targetUserId 被查看者ID
     * @return 是否已解锁
     */
    boolean hasUnlocked(Integer viewerId, Integer targetUserId);

    /**
     * 解锁画像
     *
     * @param viewerId     查看者ID
     * @param targetUserId 被查看者ID
     * @param snapshotId   画像快照ID
     * @param payType      支付方式
     * @param payAmount    支付金额
     * @return 解锁记录
     */
    PersonaViewRecordEntity unlockPersona(Integer viewerId, Integer targetUserId, Integer snapshotId, 
                                          String payType, java.math.BigDecimal payAmount);
}
