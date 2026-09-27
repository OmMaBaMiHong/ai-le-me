package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.ActivityChatGroupService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/xiangqin/group")
@Tag(name = "App端——活动群聊")
public class AppActivityChatController {

    @Resource
    private ActivityChatGroupService activityChatGroupService;

    @Resource
    private MiniAppFilingFeatureService miniAppFilingFeatureService;

    @Login
    @GetMapping("/meta")
    @Operation(summary = "活动群聊信息")
    public R meta(@RequestParam Integer activityId,
                  @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireHongniangEnabled();
        return R.ok().put("data", activityChatGroupService.getGroupMeta(activityId, user));
    }

    @Login
    @PostMapping("/create")
    @Operation(summary = "创建或打开活动群聊")
    public R create(@RequestBody Map<String, Object> params,
                    @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireHongniangEnabled();
        Integer activityId = Integer.valueOf(String.valueOf(params.get("activityId")));
        return R.ok().put("data", activityChatGroupService.createOrOpenGroup(activityId, user));
    }

    @Login
    @GetMapping("/sessionList")
    @Operation(summary = "活动群聊会话列表")
    public R sessionList(@Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (!miniAppFilingFeatureService.isHongniangEnabled()) {
            return R.ok().put("list", List.of());
        }
        return R.ok().put("list", activityChatGroupService.getUserSessionList(user));
    }

    @Login
    @GetMapping("/messages")
    @Operation(summary = "活动群聊消息列表")
    public R messages(@RequestParam Integer groupId,
                      @RequestParam(required = false) Long lastMessageId,
                      @RequestParam(required = false) Integer pageSize,
                      @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireHongniangEnabled();
        List<Map<String, Object>> list = activityChatGroupService.getMessageList(groupId, user, lastMessageId, pageSize);
        return R.ok().put("list", list);
    }

    @Login
    @PostMapping("/send")
    @Operation(summary = "发送活动群聊消息")
    public R send(@RequestBody Map<String, Object> params,
                  @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireHongniangEnabled();
        Integer groupId = Integer.valueOf(String.valueOf(params.get("groupId")));
        String content = params.get("content") == null ? "" : String.valueOf(params.get("content"));
        String messageType = params.get("messageType") == null ? "text" : String.valueOf(params.get("messageType"));
        return R.ok().put("data", activityChatGroupService.sendMessage(groupId, user, content, messageType));
    }
}
