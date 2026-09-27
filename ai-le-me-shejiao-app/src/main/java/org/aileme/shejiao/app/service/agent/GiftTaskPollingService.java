package org.aileme.shejiao.app.service.agent;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.SysQuartzJobService;
import org.aileme.shejiao.app.dao.GiftTaskDao;
import org.aileme.shejiao.domain.entity.app.GiftTaskEntity;
import org.aileme.shejiao.domain.entity.job.SysQuartzJob;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GiftTaskPollingService {

    public static final String JOB_CODE = "ai_gift_polling";

    @Autowired
    private GiftTaskDao giftTaskDao;

    @Autowired
    private GiftExecuteService giftExecuteService;

    @Autowired
    @Lazy
    private SysQuartzJobService sysQuartzJobService;

    @Value("${shejiao.ai-gift.polling.timeout-minutes:20}")
    private int timeoutMinutes;

    @Value("${shejiao.ai-gift.accept-timeout-hours:24}")
    private int acceptTimeoutHours;

    @Value("${shejiao.ai-gift.polling.batch-size:2}")
    private int batchSize;

    public String doPollPendingTasks() {
        failTimeoutTasks();
        expirePendingAcceptTasks();

        List<GiftTaskEntity> pendingTasks = giftTaskDao.selectList(new LambdaQueryWrapper<GiftTaskEntity>()
                .eq(GiftTaskEntity::getGiftStatus, GiftExecuteService.GIFT_STATUS_ACCEPTED)
                .eq(GiftTaskEntity::getTaskStatus, GiftExecuteService.TASK_STATUS_PENDING)
                .orderByAsc(GiftTaskEntity::getId)
                .last("limit " + Math.max(1, batchSize)));

        if (pendingTasks == null || pendingTasks.isEmpty()) {
            return "无待处理礼物任务";
        }

        int successCount = 0;
        int failCount = 0;
        for (GiftTaskEntity task : pendingTasks) {
            try {
                giftExecuteService.processTask(task.getId());
                successCount++;
            } catch (Exception ex) {
                failCount++;
                log.error("[gift-task-polling] process failed. taskId={}, reason={}", task.getId(), ex.getMessage(), ex);
            }
        }
        String result = String.format("处理%d个礼物任务: %d个已处理, %d个异常", pendingTasks.size(), successCount, failCount);
        log.info("[gift-task-polling] {}", result);
        return result;
    }

    @DSTransactional
    public Map<String, Object> ensureDefaultQuartzJob() {
        SysQuartzJob job = sysQuartzJobService.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .eq(SysQuartzJob::getJobCode, JOB_CODE)
                .last("limit 1")
                .one();
        boolean created = false;
        if (job == null) {
            job = new SysQuartzJob();
            job.setJobName("AI礼物任务轮询");
            job.setJobGroup("AI");
            job.setJobCode(JOB_CODE);
            job.setCronExpression("0/10 * * * * ?");
            job.setJobParams("");
            job.setAllowConcurrent(0);
            job.setStatus(SysQuartzJob.STATUS_ENABLED);
            job.setRemark("默认启用，异步处理送礼后的礼物图生成任务。");
            sysQuartzJobService.saveJob(job);
            created = true;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("jobId", job.getId());
        result.put("jobCode", job.getJobCode());
        result.put("status", job.getStatus());
        result.put("cronExpression", job.getCronExpression());
        return result;
    }

    private void failTimeoutTasks() {
        long timeoutMillis = Math.max(5, timeoutMinutes) * 60L * 1000L;
        Date now = new Date();
        List<GiftTaskEntity> processingTasks = giftTaskDao.selectList(new LambdaQueryWrapper<GiftTaskEntity>()
                .eq(GiftTaskEntity::getTaskStatus, GiftExecuteService.TASK_STATUS_PROCESSING));
        for (GiftTaskEntity task : processingTasks) {
            if (task.getUpdateTime() == null) {
                continue;
            }
            long elapsed = now.getTime() - task.getUpdateTime().getTime();
            if (elapsed < timeoutMillis) {
                continue;
            }
            task.setTaskStatus(GiftExecuteService.TASK_STATUS_FAILED);
            task.setStatusNote(StringUtils.abbreviate("心意已送达，但礼物图生成超时，请稍后重试", 120));
            task.setUpdateTime(now);
            giftTaskDao.updateById(task);
            log.warn("[gift-task-polling] task timeout. taskId={}, elapsedMs={}", task.getId(), elapsed);
        }
    }

    private void expirePendingAcceptTasks() {
        long timeoutMillis = Math.max(1, acceptTimeoutHours) * 60L * 60L * 1000L;
        Date now = new Date();
        List<GiftTaskEntity> pendingAcceptTasks = giftTaskDao.selectList(new LambdaQueryWrapper<GiftTaskEntity>()
                .eq(GiftTaskEntity::getGiftStatus, GiftExecuteService.GIFT_STATUS_PENDING_ACCEPT));
        for (GiftTaskEntity task : pendingAcceptTasks) {
            if (task.getCreateTime() == null) {
                continue;
            }
            long elapsed = now.getTime() - task.getCreateTime().getTime();
            if (elapsed < timeoutMillis) {
                continue;
            }
            giftExecuteService.expireTask(task.getId());
            log.warn("[gift-task-polling] task accept timeout. taskId={}, elapsedMs={}", task.getId(), elapsed);
        }
    }
}
