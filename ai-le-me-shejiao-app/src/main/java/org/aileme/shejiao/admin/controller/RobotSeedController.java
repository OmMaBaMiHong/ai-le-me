package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.RobotSeedService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.R;

import java.util.Map;

@RestController
@RequestMapping("/admin/robotSeed")
@Tag(name = "管理端——机器人用户与内容造数")
public class RobotSeedController {

    private final RobotSeedService robotSeedService;

    public RobotSeedController(RobotSeedService robotSeedService) {
        this.robotSeedService = robotSeedService;
    }

    @SysLog("批量生成机器人用户与动态")
    @PostMapping("/batchUsers")
    @Operation(summary = "批量生成机器人用户并发动态")
    public R batchUsers(@RequestParam(defaultValue = "100") Integer userCount,
                        @RequestParam(defaultValue = "1") Integer minPostsPerUser,
                        @RequestParam(defaultValue = "2") Integer maxPostsPerUser) {
        return R.ok().put("data", robotSeedService.generateBatchUsersWithPosts(userCount, minPostsPerUser, maxPostsPerUser));
    }

    @SysLog("按现有用户批量生成动态")
    @PostMapping("/batchExistingUsersPosts")
    @Operation(summary = "按现有用户资料批量生成图文/视频/投票/长文")
    public R batchExistingUsersPosts(@RequestBody(required = false) Map<String, Object> options) {
        return R.ok().put("data", robotSeedService.generatePostsForExistingUsers(options));
    }

    @SysLog("按现有头像刷新用户形象图")
    @PostMapping("/refreshExistingUserFigures")
    @Operation(summary = "按现有用户头像重生成 1-3 张形象图，默认只处理脏数据/重复图")
    public R refreshExistingUserFigures(@RequestBody(required = false) Map<String, Object> options) {
        return R.ok().put("data", robotSeedService.refreshExistingUserFigures(options));
    }

    @SysLog("生成单条机器人内容")
    @PostMapping("/singlePost")
    @Operation(summary = "单次生成一条机器人内容")
    public R singlePost(@RequestParam(defaultValue = "true") Boolean createUserIfNeeded) {
        return R.ok().put("data", robotSeedService.generateSinglePost(Boolean.TRUE.equals(createUserIfNeeded)));
    }

    @SysLog("按高级参数生成单条机器人内容")
    @PostMapping("/singlePostWithOptions")
    @Operation(summary = "按高级参数生成单条机器人内容，支持 local/hybrid/ai 模式")
    public R singlePostWithOptions(@RequestBody(required = false) Map<String, Object> options) {
        return R.ok().put("data", robotSeedService.generateSinglePostWithOptions(options));
    }

    @SysLog("初始化机器人默认Quartz任务")
    @PostMapping("/ensureSinglePostJob")
    @Operation(summary = "确保默认暂停的单条机器人内容任务存在")
    public R ensureSinglePostJob() {
        return R.ok().put("data", robotSeedService.ensureDefaultSinglePostQuartzJob());
    }

    @SysLog("初始化机器人AI内容工厂Quartz任务")
    @PostMapping("/ensureAiFactoryJob")
    @Operation(summary = "确保默认暂停的 AI 内容工厂机器人任务存在")
    public R ensureAiFactoryJob() {
        return R.ok().put("data", robotSeedService.ensureDefaultAiFactoryQuartzJob());
    }
}
