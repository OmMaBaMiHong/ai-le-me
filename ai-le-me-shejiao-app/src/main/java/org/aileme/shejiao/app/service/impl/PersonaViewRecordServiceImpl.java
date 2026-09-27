package org.aileme.shejiao.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.api.service.PersonaViewRecordService;
import org.aileme.shejiao.app.dao.PersonaViewRecordDao;
import org.aileme.shejiao.domain.entity.admin.PersonaViewRecordEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 画像查看记录服务实现
 *
 * @author system
 * @date 2026-02-14
 */
@Slf4j
@Service
public class PersonaViewRecordServiceImpl extends ServiceImpl<PersonaViewRecordDao, PersonaViewRecordEntity>
        implements PersonaViewRecordService {

    @Override
    public boolean hasUnlocked(Integer viewerId, Integer targetUserId) {
        Long count = this.lambdaQuery()
                .eq(PersonaViewRecordEntity::getViewerId, viewerId)
                .eq(PersonaViewRecordEntity::getTargetUserId, targetUserId)
                .eq(PersonaViewRecordEntity::getStatus, 1)
                .count();
        return count > 0;
    }

    @Override
    @DSTransactional
    public PersonaViewRecordEntity unlockPersona(Integer viewerId, Integer targetUserId, Integer snapshotId,
                                                  String payType, BigDecimal payAmount) {
        log.info("解锁画像: viewerId={}, targetUserId={}, snapshotId={}", viewerId, targetUserId, snapshotId);

        // 检查是否已解锁
        if (hasUnlocked(viewerId, targetUserId)) {
            PersonaViewRecordEntity existing = this.lambdaQuery()
                    .eq(PersonaViewRecordEntity::getViewerId, viewerId)
                    .eq(PersonaViewRecordEntity::getTargetUserId, targetUserId)
                    .one();
            log.info("用户已解锁该画像，直接返回: recordId={}", existing.getId());
            return existing;
        }

        // 创建解锁记录
        PersonaViewRecordEntity record = new PersonaViewRecordEntity();
        record.setViewerId(viewerId);
        record.setTargetUserId(targetUserId);
        record.setSnapshotId(snapshotId);
        record.setPayType(payType);
        record.setPayAmount(payAmount);
        record.setViewTime(LocalDateTime.now());
        record.setExpireTime(null); // NULL表示永久
        record.setStatus(1);

        this.save(record);

        log.info("画像解锁成功: recordId={}", record.getId());
        return record;
    }
}
