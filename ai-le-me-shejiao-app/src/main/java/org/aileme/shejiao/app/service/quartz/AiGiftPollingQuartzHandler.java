package org.aileme.shejiao.app.service.quartz;

import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.agent.GiftTaskPollingService;

@Component
public class AiGiftPollingQuartzHandler implements QuartzTaskHandler {

    private final GiftTaskPollingService giftTaskPollingService;

    public AiGiftPollingQuartzHandler(GiftTaskPollingService giftTaskPollingService) {
        this.giftTaskPollingService = giftTaskPollingService;
    }

    @Override
    public String getJobCode() {
        return GiftTaskPollingService.JOB_CODE;
    }

    @Override
    public String getJobName() {
        return "AI礼物任务轮询";
    }

    @Override
    public String getDescription() {
        return "异步处理礼物图生成任务，自动同步生成状态与结果。";
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
        return giftTaskPollingService.doPollPendingTasks();
    }
}
