package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.app.service.agent.GiftExecuteService;
import org.aileme.shejiao.app.service.impl.SocialIntentService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.ConfirmGiftIntentForm;
import org.aileme.shejiao.domain.param.app.ConfirmWechatIntentForm;
import org.aileme.shejiao.domain.param.app.RejectGiftIntentForm;
import org.aileme.shejiao.domain.param.app.RejectWechatIntentForm;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;

import java.util.Map;

@Tag(name = "移动端——社交意图")
@RestController
@RequestMapping("app/socialIntent")
public class AppSocialIntentController {

    @Autowired
    private SocialIntentService socialIntentService;

    @Autowired
    private GiftExecuteService giftExecuteService;

    @Autowired
    private MiniAppFilingFeatureService miniAppFilingFeatureService;

    @Login
    @PostMapping("/confirmWechat")
    @Operation(summary = "确认微信申请并发送微信号")
    public Result<Map<String, Object>> confirmWechat(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                                     @RequestBody ConfirmWechatIntentForm request) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        Map<String, Object> result = socialIntentService.confirmWechat(user, request);
        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @PostMapping("/rejectWechat")
    @Operation(summary = "拒绝微信申请")
    public Result<Map<String, Object>> rejectWechat(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                                    @RequestBody RejectWechatIntentForm request) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        Map<String, Object> result = socialIntentService.rejectWechat(user, request);
        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @PostMapping("/confirmMatchRequest")
    @Operation(summary = "确认红娘牵线申请")
    public Result<Map<String, Object>> confirmMatchRequest(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                                           @RequestBody Map<String, Object> request) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        Map<String, Object> result = socialIntentService.confirmMatchRequest(user, String.valueOf(request.get("requestId")));
        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @PostMapping("/rejectMatchRequest")
    @Operation(summary = "拒绝红娘牵线申请")
    public Result<Map<String, Object>> rejectMatchRequest(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                                          @RequestBody Map<String, Object> request) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        Map<String, Object> result = socialIntentService.rejectMatchRequest(
                user,
                String.valueOf(request.get("requestId")),
                request.get("reason") == null ? null : String.valueOf(request.get("reason"))
        );
        return new Result<Map<String, Object>>().ok(result);
    }

    @Login
    @PostMapping("/confirmGift")
    @Operation(summary = "确认礼物申请并成为好友")
    public Result<AgentGiftExecuteVo> confirmGift(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                                  @RequestBody ConfirmGiftIntentForm request) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        AgentGiftExecuteVo result = giftExecuteService.confirmGift(
                user,
                request.getRequestId(),
                request.getRequesterUid(),
                request.getSessionId()
        );
        return new Result<AgentGiftExecuteVo>().ok(result);
    }

    @Login
    @PostMapping("/rejectGift")
    @Operation(summary = "婉拒礼物申请")
    public Result<AgentGiftExecuteVo> rejectGift(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                                 @RequestBody RejectGiftIntentForm request) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        AgentGiftExecuteVo result = giftExecuteService.rejectGift(
                user,
                request.getRequestId(),
                request.getRequesterUid(),
                request.getSessionId()
        );
        return new Result<AgentGiftExecuteVo>().ok(result);
    }
}
