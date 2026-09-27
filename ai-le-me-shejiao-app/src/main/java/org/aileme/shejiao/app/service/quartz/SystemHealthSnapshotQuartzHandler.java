package org.aileme.shejiao.app.service.quartz;

import org.aileme.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SystemHealthSnapshotQuartzHandler implements QuartzTaskHandler {

    @Override
    public String getJobCode() {
        return "system_health_snapshot";
    }

    @Override
    public String getJobName() {
        return "系统健康快照";
    }

    @Override
    public String getDescription() {
        return "输出当前时间、JVM 内存与活跃线程数，用于 Quartz 调度链路自检。";
    }

    @Override
    public String getDefaultCronExpression() {
        return "0 0/5 * * * ?";
    }

    @Override
    public String getDefaultJobGroup() {
        return "SYSTEM";
    }

    @Override
    public String execute(String jobParams) {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("timestamp", LocalDateTime.now().toString());
        snapshot.put("availableProcessors", runtime.availableProcessors());
        snapshot.put("freeMemory", runtime.freeMemory());
        snapshot.put("totalMemory", runtime.totalMemory());
        snapshot.put("maxMemory", runtime.maxMemory());
        snapshot.put("activeThreads", Thread.activeCount());
        return JsonUtils.toJsonString(snapshot);
    }
}
