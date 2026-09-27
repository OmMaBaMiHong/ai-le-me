package org.aileme.shejiao.app.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.RecommendLoveService;
import org.aileme.shejiao.api.service.UserPersonaSnapshotService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;

import java.time.Duration;

@Slf4j
@Service
public class PersonaPreGenerationService {

    private static final Duration AUTO_REFRESH_LOCK_TTL = Duration.ofMinutes(15);
    private static final String PERSONA_AUTO_REFRESH_LOCK_KEY = "persona:auto:refresh:";

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private UserPersonaSnapshotService userPersonaSnapshotService;

    @Autowired
    private RecommendLoveService recommendLoveService;

    @Lazy
    @Autowired
    private PersonaPreGenerationService self;

    public boolean triggerProfileRefresh(Integer userId, String source) {
        if (userId == null || userId <= 0) {
            return false;
        }
        String lockKey = buildLockKey(userId);
        boolean accepted = RedisUtils.setObjectIfAbsent(lockKey, "1", AUTO_REFRESH_LOCK_TTL);
        if (!accepted) {
            return false;
        }
        self.runProfileRefreshAsync(userId, source);
        return true;
    }

    @Async
    public void runProfileRefreshAsync(Integer userId, String source) {
        String lockKey = buildLockKey(userId);
        try {
            AppUserEntity user = appUserService.getById(userId);
            if (user == null) {
                return;
            }
            UserPersonaSnapshotEntity snapshot = userPersonaSnapshotService.generatePersona(
                    userId,
                    StringUtils.defaultIfBlank(source, "profile_auto_refresh")
            );
            if (snapshot == null) {
                return;
            }
            recommendLoveService.preGenerateSmartMatch(user);
            log.info("资料完善后自动预生成完成, userId={}, snapshotId={}", userId, snapshot.getId());
        } catch (LinfengException ex) {
            log.info("资料尚未达到自动画像门槛, userId={}, msg={}", userId, ex.getMessage());
        } catch (Exception ex) {
            log.error("资料自动预生成画像失败, userId={}", userId, ex);
        } finally {
            RedisUtils.deleteObject(lockKey);
        }
    }

    private String buildLockKey(Integer userId) {
        return PERSONA_AUTO_REFRESH_LOCK_KEY + userId;
    }
}
