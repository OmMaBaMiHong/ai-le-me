package org.aileme.shejiao.app.service.quartz;

import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.ai.video.VideoTaskPollingService;

@Component
public class AiVideoPollingQuartzHandler implements QuartzTaskHandler {

    private final VideoTaskPollingService videoTaskPollingService;

    public AiVideoPollingQuartzHandler(VideoTaskPollingService videoTaskPollingService) {
        this.videoTaskPollingService = videoTaskPollingService;
    }

    @Override
    public String getJobCode() {
        return "ai_video_polling";
    }

    @Override
    public String getJobName() {
        return "AI视频任务轮询";
    }

    @Override
    public String getDescription() {
        return "轮询 AI 视频生成任务，自动同步处理中与完成状态。";
    }

    @Override
    public String getDefaultCronExpression() {
        return "0/10 * * * * ?";
    }

    @Override
    public String getDefaultJobGroup() {
        return "AI";
    }

    @Override
    public String execute(String jobParams) {
        return videoTaskPollingService.doPollPendingTasks();
    }
}
