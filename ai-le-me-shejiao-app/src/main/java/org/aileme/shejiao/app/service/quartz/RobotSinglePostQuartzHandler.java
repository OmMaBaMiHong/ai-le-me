package org.aileme.shejiao.app.service.quartz;

import org.aileme.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.api.service.RobotSeedService;

import java.util.Map;

@Component
public class RobotSinglePostQuartzHandler implements QuartzTaskHandler {

    private final RobotSeedService robotSeedService;

    public RobotSinglePostQuartzHandler(RobotSeedService robotSeedService) {
        this.robotSeedService = robotSeedService;
    }

    @Override
    public String getJobCode() {
        return "robot_single_post_generation";
    }

    @Override
    public String getJobName() {
        return "机器人单条内容生成";
    }

    @Override
    public String getDescription() {
        return "每次生成一条机器人内容，默认两小时一次，任务创建后默认暂停。";
    }

    @Override
    public String getDefaultCronExpression() {
        return "0 0 0/2 * * ?";
    }

    @Override
    public String getDefaultJobGroup() {
        return "ROBOT";
    }

    @Override
    public String execute(String jobParams) {
        Map<String, Object> result = robotSeedService.generateSinglePost(true);
        return JsonUtils.toJsonString(result);
    }
}
