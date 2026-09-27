package org.aileme.shejiao.app.service.quartz;

import org.aileme.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.api.service.RobotSeedService;

import java.util.Map;

@Component
public class RobotAiFactoryQuartzHandler implements QuartzTaskHandler {

    private final RobotSeedService robotSeedService;

    public RobotAiFactoryQuartzHandler(RobotSeedService robotSeedService) {
        this.robotSeedService = robotSeedService;
    }

    @Override
    public String getJobCode() {
        return "robot_ai_factory_generation";
    }

    @Override
    public String getJobName() {
        return "机器人内容工厂";
    }

    @Override
    public String getDescription() {
        return "默认走本地内容工厂，可通过 jobParams 传 contentFactoryMode、preferredProvider、preferredModel 切换为 AI 或 hybrid 模式，默认两小时一次且任务创建后默认暂停。";
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
        Map<String, Object> result = robotSeedService.generateSinglePostWithOptions(JsonUtils.parseObject(jobParams, Map.class));
        return JsonUtils.toJsonString(result);
    }
}
