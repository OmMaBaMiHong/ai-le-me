package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.CronExpression;
import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.admin.dao.SysQuartzJobDao;
import org.aileme.shejiao.admin.dao.SysQuartzJobLogDao;
import org.aileme.shejiao.api.service.SysQuartzJobService;
import org.aileme.shejiao.app.service.quartz.QuartzManagedJob;
import org.aileme.shejiao.app.service.quartz.QuartzManagedNonConcurrentJob;
import org.aileme.shejiao.app.service.quartz.QuartzTaskHandlerRegistry;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.job.SysQuartzJob;
import org.aileme.shejiao.domain.entity.job.SysQuartzJobLog;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@DS("master")
@Service("sysQuartzJobService")
public class SysQuartzJobServiceImpl extends ServiceImpl<SysQuartzJobDao, SysQuartzJob> implements SysQuartzJobService {

    private static final String QUARTZ_GROUP_PREFIX = "YUELAO_";
    private static final String QUARTZ_JOB_PREFIX = "YUELAO_JOB_";
    private static final String QUARTZ_TRIGGER_PREFIX = "YUELAO_TRIGGER_";

    private final Scheduler scheduler;
    private final SysQuartzJobLogDao quartzJobLogDao;
    private final QuartzTaskHandlerRegistry handlerRegistry;

    public SysQuartzJobServiceImpl(Scheduler scheduler,
                                   SysQuartzJobLogDao quartzJobLogDao,
                                   QuartzTaskHandlerRegistry handlerRegistry) {
        this.scheduler = scheduler;
        this.quartzJobLogDao = quartzJobLogDao;
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SysQuartzJob> wrapper = new LambdaQueryWrapper<>();
        String key = stringParam(params, "key");
        String status = stringParam(params, "status");
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(w -> w.like(SysQuartzJob::getJobName, key)
                    .or()
                    .like(SysQuartzJob::getJobCode, key)
                    .or()
                    .like(SysQuartzJob::getJobGroup, key));
        }
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(SysQuartzJob::getStatus, Integer.parseInt(status));
        }
        wrapper.eq(SysQuartzJob::getDeleted, 0).orderByDesc(SysQuartzJob::getId);
        IPage<SysQuartzJob> page = this.page(new Query<SysQuartzJob>().getPage(params), wrapper);
        page.getRecords().forEach(this::fillRuntimeInfo);
        return new PageUtils(page);
    }

    @Override
    public PageUtils queryLogPage(Map<String, Object> params) {
        LambdaQueryWrapper<SysQuartzJobLog> wrapper = new LambdaQueryWrapper<>();
        String jobId = stringParam(params, "jobId");
        if (StringUtils.isNotBlank(jobId)) {
            wrapper.eq(SysQuartzJobLog::getJobId, Long.parseLong(jobId));
        }
        wrapper.orderByDesc(SysQuartzJobLog::getId);
        IPage<SysQuartzJobLog> page = quartzJobLogDao.selectPage(new Query<SysQuartzJobLog>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public SysQuartzJob getDetail(Long id) {
        SysQuartzJob job = getRequiredJob(id);
        fillRuntimeInfo(job);
        return job;
    }

    @Override
    public void saveJob(SysQuartzJob job) {
        normalizeAndValidate(job, null);
        if (!this.save(job)) {
            throw new LinfengException("保存 Quartz 任务失败");
        }
        refreshScheduler(job);
    }

    @Override
    public void updateJob(SysQuartzJob job) {
        if (job.getId() == null) {
            throw new LinfengException("任务ID不能为空");
        }
        SysQuartzJob existing = getRequiredJob(job.getId());
        normalizeAndValidate(job, existing.getId());
        job.setCreateTime(existing.getCreateTime());
        if (!this.updateById(job)) {
            throw new LinfengException("更新 Quartz 任务失败");
        }
        refreshScheduler(job);
    }

    @Override
    public void deleteJobs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            try {
                scheduler.deleteJob(buildJobKey(id));
            } catch (SchedulerException e) {
                throw new LinfengException("删除 Quartz 调度失败: " + id, e);
            }
        }
        this.removeByIds(ids);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysQuartzJob job = getRequiredJob(id);
        job.setStatus(status);
        if (!this.updateById(job)) {
            throw new LinfengException("更新任务状态失败");
        }
        refreshScheduler(job);
    }

    @Override
    public void runOnce(Long id) {
        SysQuartzJob job = getRequiredJob(id);
        long start = System.currentTimeMillis();
        try {
            String result = handlerRegistry.getRequiredHandler(job.getJobCode()).execute(job.getJobParams());
            recordExecution(job.getId(), job.getJobCode(), result, null, start);
        } catch (Exception e) {
            recordExecution(job.getId(), job.getJobCode(), null, e.getMessage(), start);
            throw new LinfengException("立即执行任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void syncAllJobs() {
        clearManagedJobs();
        List<SysQuartzJob> jobs = this.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .list();
        for (SysQuartzJob job : jobs) {
            refreshScheduler(job);
        }
    }

    @Override
    public Map<String, Object> getJobRuntime(String jobCode) {
        SysQuartzJob job = this.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .eq(SysQuartzJob::getJobCode, StringUtils.trimToEmpty(jobCode))
                .last("limit 1")
                .one();
        if (job == null) {
            return Map.of(
                    "exists", false,
                    "enabled", false
            );
        }
        fillRuntimeInfo(job);
        Map<String, Object> result = new HashMap<>();
        result.put("exists", true);
        result.put("enabled", SysQuartzJob.STATUS_ENABLED == safeInt(job.getStatus(), SysQuartzJob.STATUS_PAUSED));
        result.put("jobId", job.getId());
        result.put("jobCode", job.getJobCode());
        result.put("jobName", job.getJobName());
        result.put("cronExpression", StringUtils.defaultString(job.getCronExpression()));
        result.put("nextFireTime", job.getNextFireTime());
        result.put("previousFireTime", job.getPreviousFireTime());
        return result;
    }

    @Override
    public void recordExecution(Long jobId, String jobCode, String result, String errorMessage, long startTimestamp) {
        SysQuartzJob job = jobId == null ? null : this.getById(jobId);
        LocalDateTime startTime = LocalDateTime.ofInstant(new Date(startTimestamp).toInstant(), ZoneId.systemDefault());
        LocalDateTime endTime = LocalDateTime.now();
        SysQuartzJobLog logEntity = new SysQuartzJobLog();
        logEntity.setJobId(jobId);
        logEntity.setJobName(job != null ? job.getJobName() : jobCode);
        logEntity.setJobCode(jobCode);
        logEntity.setExecuteStatus(StringUtils.isBlank(errorMessage) ? SysQuartzJobLog.STATUS_SUCCESS : SysQuartzJobLog.STATUS_FAIL);
        logEntity.setResultSummary(StringUtils.abbreviate(StringUtils.defaultString(result), 500));
        logEntity.setErrorMessage(StringUtils.abbreviate(StringUtils.defaultString(errorMessage), 1000));
        logEntity.setDurationMs(Math.max(0L, System.currentTimeMillis() - startTimestamp));
        logEntity.setStartTime(startTime);
        logEntity.setEndTime(endTime);
        logEntity.setCreateTime(endTime);
        quartzJobLogDao.insert(logEntity);
    }

    private void normalizeAndValidate(SysQuartzJob job, Long excludeId) {
        if (job == null) {
            throw new LinfengException("任务不能为空");
        }
        job.setJobName(StringUtils.trimToEmpty(job.getJobName()));
        job.setJobGroup(StringUtils.defaultIfBlank(StringUtils.trimToEmpty(job.getJobGroup()), "SYSTEM"));
        job.setJobCode(StringUtils.trimToEmpty(job.getJobCode()).toLowerCase());
        job.setCronExpression(StringUtils.trimToEmpty(job.getCronExpression()));
        job.setJobParams(StringUtils.trimToEmpty(job.getJobParams()));
        job.setAllowConcurrent(safeInt(job.getAllowConcurrent(), 0));
        job.setStatus(safeInt(job.getStatus(), SysQuartzJob.STATUS_ENABLED));
        job.setRemark(StringUtils.trimToEmpty(job.getRemark()));
        job.setDeleted(0);

        if (StringUtils.isBlank(job.getJobName())) {
            throw new LinfengException("任务名称不能为空");
        }
        if (StringUtils.isBlank(job.getJobCode())) {
            throw new LinfengException("任务编码不能为空");
        }
        if (StringUtils.isBlank(job.getCronExpression()) || !CronExpression.isValidExpression(job.getCronExpression())) {
            throw new LinfengException("Cron 表达式不合法");
        }
        handlerRegistry.getRequiredHandler(job.getJobCode());

        boolean exists = this.lambdaQuery()
                .eq(SysQuartzJob::getDeleted, 0)
                .eq(SysQuartzJob::getJobCode, job.getJobCode())
                .ne(excludeId != null, SysQuartzJob::getId, excludeId)
                .count() > 0;
        if (exists) {
            throw new LinfengException("任务编码已存在: " + job.getJobCode());
        }
    }

    private void refreshScheduler(SysQuartzJob job) {
        try {
            JobKey jobKey = buildJobKey(job.getId());
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDetail jobDetail = JobBuilder.newJob(selectQuartzJobClass(job))
                    .withIdentity(jobKey)
                    .usingJobData(buildJobDataMap(job))
                    .build();

            TriggerKey triggerKey = buildTriggerKey(job.getId());
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                    .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            if (safeInt(job.getStatus(), SysQuartzJob.STATUS_PAUSED) == SysQuartzJob.STATUS_PAUSED) {
                scheduler.pauseJob(jobKey);
            } else {
                scheduler.resumeJob(jobKey);
            }
        } catch (SchedulerException e) {
            throw new LinfengException("刷新 Quartz 调度失败: " + job.getJobName(), e);
        }
    }

    private JobDataMap buildJobDataMap(SysQuartzJob job) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobId", job.getId());
        dataMap.put("jobCode", job.getJobCode());
        dataMap.put("jobParams", StringUtils.defaultString(job.getJobParams()));
        dataMap.put("jobName", StringUtils.defaultString(job.getJobName()));
        return dataMap;
    }

    private Class<? extends org.quartz.Job> selectQuartzJobClass(SysQuartzJob job) {
        return safeInt(job.getAllowConcurrent(), 0) == 1
                ? QuartzManagedJob.class
                : QuartzManagedNonConcurrentJob.class;
    }

    private void fillRuntimeInfo(SysQuartzJob job) {
        if (job == null || job.getId() == null) {
            return;
        }
        try {
            Trigger trigger = scheduler.getTrigger(buildTriggerKey(job.getId()));
            if (trigger != null) {
                job.setNextFireTime(trigger.getNextFireTime());
                job.setPreviousFireTime(trigger.getPreviousFireTime());
            } else {
                job.setNextFireTime(null);
                job.setPreviousFireTime(null);
            }
        } catch (SchedulerException e) {
            log.warn("读取 Quartz 运行态失败, jobId={}, error={}", job.getId(), e.getMessage());
        }
    }

    private void clearManagedJobs() {
        try {
            List<JobKey> managedKeys = new ArrayList<>();
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(groupName))) {
                    if (StringUtils.startsWith(jobKey.getName(), QUARTZ_JOB_PREFIX)) {
                        managedKeys.add(jobKey);
                    }
                }
            }
            for (JobKey managedKey : managedKeys) {
                scheduler.deleteJob(managedKey);
            }
        } catch (SchedulerException e) {
            throw new LinfengException("清理 Quartz 内存任务失败", e);
        }
    }

    private SysQuartzJob getRequiredJob(Long id) {
        SysQuartzJob job = this.getById(id);
        if (job == null) {
            throw new LinfengException("任务不存在: " + id);
        }
        return job;
    }

    private JobKey buildJobKey(Long id) {
        return JobKey.jobKey(QUARTZ_JOB_PREFIX + id, QUARTZ_GROUP_PREFIX + "TASK");
    }

    private TriggerKey buildTriggerKey(Long id) {
        return TriggerKey.triggerKey(QUARTZ_TRIGGER_PREFIX + id, QUARTZ_GROUP_PREFIX + "TASK");
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String stringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
