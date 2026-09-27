package org.aileme.shejiao.app.service.quartz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.api.service.RobotSeedService;
import org.aileme.shejiao.api.service.SysQuartzJobService;
import org.aileme.shejiao.app.service.agent.GiftTaskPollingService;
import org.aileme.shejiao.app.service.agent.SmartMatchReplenishService;

@Slf4j
@Component
public class QuartzJobBootstrap implements ApplicationRunner {

    private final SysQuartzJobService quartzJobService;
    private final RobotSeedService robotSeedService;
    private final GiftTaskPollingService giftTaskPollingService;
    private final SmartMatchReplenishService smartMatchReplenishService;

    public QuartzJobBootstrap(SysQuartzJobService quartzJobService,
                              RobotSeedService robotSeedService,
                              GiftTaskPollingService giftTaskPollingService,
                              SmartMatchReplenishService smartMatchReplenishService) {
        this.quartzJobService = quartzJobService;
        this.robotSeedService = robotSeedService;
        this.giftTaskPollingService = giftTaskPollingService;
        this.smartMatchReplenishService = smartMatchReplenishService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            quartzJobService.syncAllJobs();
        } catch (Exception ex) {
            log.warn("[Quartz] 数据库任务同步失败，已跳过本轮启动同步: {}", ex.getMessage());
        }
        try {
            robotSeedService.ensureDefaultSinglePostQuartzJob();
        } catch (Exception ex) {
            log.warn("[Quartz] 默认单发帖任务初始化失败，已跳过: {}", ex.getMessage());
        }
        try {
            giftTaskPollingService.ensureDefaultQuartzJob();
        } catch (Exception ex) {
            log.warn("[Quartz] 默认礼物任务轮询初始化失败，已跳过: {}", ex.getMessage());
        }
        try {
            smartMatchReplenishService.ensureDefaultQuartzJob();
        } catch (Exception ex) {
            log.warn("[Quartz] 默认智能红娘补量任务初始化失败，已跳过: {}", ex.getMessage());
        }
        log.info("[Quartz] 启动阶段任务同步流程已完成");
    }
}
