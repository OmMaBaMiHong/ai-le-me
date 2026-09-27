package org.aileme.shejiao.app.service.quartz;

import org.springframework.stereotype.Component;
import org.aileme.shejiao.app.service.agent.SmartMatchReplenishService;

@Component
public class SmartMatchReplenishQuartzHandler implements QuartzTaskHandler {

    private final SmartMatchReplenishService smartMatchReplenishService;

    public SmartMatchReplenishQuartzHandler(SmartMatchReplenishService smartMatchReplenishService) {
        this.smartMatchReplenishService = smartMatchReplenishService;
    }

    @Override
    public String getJobCode() {
        return SmartMatchReplenishService.JOB_CODE;
    }

    @Override
    public String getJobName() {
        return "智能红娘每日补量";
    }

    @Override
    public String getDescription() {
        return "检查每日推荐名额是否还有剩余，并在快照不足时自动补足新的红娘推荐结果。";
    }

    @Override
    public String getDefaultCronExpression() {
        return "0 0/30 * * * ?";
    }

    @Override
    public String getDefaultJobGroup() {
        return "AI";
    }

    @Override
    public String execute(String jobParams) {
        return smartMatchReplenishService.replenishDailyQuota();
    }
}
