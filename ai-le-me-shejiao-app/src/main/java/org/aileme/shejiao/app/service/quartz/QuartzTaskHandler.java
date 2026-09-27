package org.aileme.shejiao.app.service.quartz;

/**
 * Quartz 任务处理器注册接口。
 * 后台只保存安全 jobCode，执行时由后端注册表路由到具体 handler。
 */
public interface QuartzTaskHandler {

    String getJobCode();

    default String getJobName() {
        return getJobCode();
    }

    default String getDescription() {
        return "";
    }

    default String getDefaultCronExpression() {
        return "0 0/5 * * * ?";
    }

    default String getDefaultJobGroup() {
        return "SYSTEM";
    }

    default Integer getDefaultAllowConcurrent() {
        return 0;
    }

    String execute(String jobParams) throws Exception;
}
