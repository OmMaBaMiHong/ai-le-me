package org.aileme.shejiao.app.service.quartz;

import org.apache.commons.lang3.StringUtils;
import org.aileme.common.core.utils.SpringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.aileme.shejiao.api.service.SysQuartzJobService;

final class QuartzManagedJobSupport {

    private QuartzManagedJobSupport() {
    }

    static void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        Long jobId = dataMap.containsKey("jobId") ? dataMap.getLong("jobId") : null;
        String jobCode = dataMap.getString("jobCode");
        String jobParams = dataMap.getString("jobParams");
        SysQuartzJobService quartzJobService = SpringUtils.getBean(SysQuartzJobService.class);
        QuartzTaskHandlerRegistry registry = SpringUtils.getBean(QuartzTaskHandlerRegistry.class);
        long start = System.currentTimeMillis();
        try {
            QuartzTaskHandler handler = registry.getRequiredHandler(jobCode);
            String result = handler.execute(jobParams);
            quartzJobService.recordExecution(jobId, jobCode, StringUtils.defaultIfBlank(result, "执行成功"), null, start);
        } catch (Exception e) {
            quartzJobService.recordExecution(jobId, jobCode, null, e.getMessage(), start);
            throw new JobExecutionException(e);
        }
    }
}
