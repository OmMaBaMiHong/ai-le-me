package org.aileme.shejiao.app.service.quartz;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.common.exception.LinfengException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuartzTaskHandlerRegistry {

    private final Map<String, QuartzTaskHandler> handlers = new LinkedHashMap<>();

    public QuartzTaskHandlerRegistry(List<QuartzTaskHandler> handlerList) {
        for (QuartzTaskHandler handler : handlerList) {
            String jobCode = StringUtils.trimToEmpty(handler.getJobCode()).toLowerCase();
            if (jobCode.isEmpty()) {
                continue;
            }
            if (handlers.containsKey(jobCode)) {
                throw new LinfengException("存在重复 Quartz jobCode: " + jobCode);
            }
            handlers.put(jobCode, handler);
        }
    }

    public QuartzTaskHandler getRequiredHandler(String jobCode) {
        QuartzTaskHandler handler = handlers.get(StringUtils.trimToEmpty(jobCode).toLowerCase());
        if (handler == null) {
            throw new LinfengException("未注册的 Quartz 任务编码: " + jobCode);
        }
        return handler;
    }

    public List<Map<String, Object>> listHandlers() {
        return handlers.values().stream()
            .map(handler -> Map.<String, Object>of(
                "jobCode", handler.getJobCode(),
                "jobName", handler.getJobName(),
                "description", handler.getDescription(),
                "defaultCronExpression", handler.getDefaultCronExpression(),
                "defaultJobGroup", handler.getDefaultJobGroup(),
                "defaultAllowConcurrent", handler.getDefaultAllowConcurrent()
            ))
            .collect(Collectors.toList());
    }
}
