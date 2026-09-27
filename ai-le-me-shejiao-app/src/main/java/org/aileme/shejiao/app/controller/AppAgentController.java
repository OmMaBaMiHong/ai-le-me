package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.app.service.agent.AgentProfileTagSuggestService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.app.service.agent.AgentAutonomyService;
import org.aileme.shejiao.app.service.agent.AgentContentCreativeService;
import org.aileme.shejiao.app.service.agent.AgentDistillationService;
import org.aileme.shejiao.app.service.agent.AgentGovernanceService;
import org.aileme.shejiao.app.service.agent.AgentOrchestratorService;
import org.aileme.shejiao.app.service.agent.AgentSuggestedActionDispatchService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.domain.vo.AgentActionTaskVo;
import org.aileme.shejiao.domain.vo.AgentActionLogVo;
import org.aileme.shejiao.domain.vo.AgentCompanionStatusVo;
import org.aileme.shejiao.domain.vo.AgentContentDraftVo;
import org.aileme.shejiao.domain.vo.AgentCoachStyleVo;
import org.aileme.shejiao.domain.vo.AgentDistillationPreviewVo;
import org.aileme.shejiao.domain.vo.AgentDistillationSceneVo;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;
import org.aileme.shejiao.domain.vo.AgentGiftPlanVo;
import org.aileme.shejiao.domain.vo.AgentMessageAutoSendVo;
import org.aileme.shejiao.domain.vo.AgentNextStepVo;
import org.aileme.shejiao.domain.vo.AgentProgramRunVo;
import org.aileme.shejiao.domain.vo.AgentRelationshipEvaluateVo;
import org.aileme.shejiao.domain.vo.AgentSuggestedActionExecuteVo;
import org.aileme.shejiao.domain.vo.AgentPermissionVo;
import org.aileme.shejiao.domain.vo.AgentSuggestionResponseVo;
import org.aileme.shejiao.domain.vo.AgentTagSuggestionVO;

import java.util.List;
import java.util.Map;

/**
 * 移动端 Agent 能力统一入口
 */
@RestController
@RequestMapping("/app/agent")
@Validated
@Tag(name = "移动端——AI Agent")
public class AppAgentController {

    @Autowired
    private AgentOrchestratorService agentOrchestratorService;

    @Autowired
    private AgentProfileTagSuggestService agentProfileTagSuggestService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AgentContentCreativeService agentContentCreativeService;

    @Autowired
    private AgentAutonomyService agentAutonomyService;

    @Autowired
    private MiniAppFilingFeatureService miniAppFilingFeatureService;

    @Autowired
    private AgentGovernanceService agentGovernanceService;

    @Autowired
    private AgentSuggestedActionDispatchService agentSuggestedActionDispatchService;

    @Autowired
    private AgentDistillationService agentDistillationService;

    @Autowired(required = false)
    private org.aileme.shejiao.app.service.agent.AgentAccessPolicyService agentAccessPolicyService;

    @Login
    @GetMapping("/distillation/scenes")
    @Operation(summary = "获取人物蒸馏场景")
    public Result<List<AgentDistillationSceneVo>> distillationScenes() {
        return new Result<List<AgentDistillationSceneVo>>().ok(agentDistillationService.listScenes());
    }

    @Login
    @PostMapping("/distillation/generate")
    @Operation(summary = "生成人物蒸馏结果")
    public Result<AgentDistillationPreviewVo> generateDistillation(
            @RequestBody(required = false) AgentDistillationGenerateForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentDistillationPreviewVo>().ok(agentDistillationService.generate(user, form));
    }

    @Login
    @GetMapping("/distillation/latest")
    @Operation(summary = "获取最近一次人物蒸馏结果")
    public Result<AgentDistillationPreviewVo> latestDistillation(
            @RequestParam(value = "sceneType", required = false) String sceneType,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentDistillationPreviewVo>().ok(agentDistillationService.getLatest(user, sceneType));
    }

    @Login
    @PostMapping("/icebreak/suggestions")
    @Operation(summary = "获取AI破冰建议")
    public Result<AgentSuggestionResponseVo> icebreakSuggestions(
            @Valid @RequestBody AgentIcebreakForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AgentSuggestionResponseVo response = agentOrchestratorService.icebreak(user, form);
        return new Result<AgentSuggestionResponseVo>().ok(response);
    }

    @Login
    @PostMapping("/reply/suggestions")
    @Operation(summary = "获取AI续聊建议")
    public Result<AgentSuggestionResponseVo> replySuggestions(
            @Valid @RequestBody AgentReplyForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AgentSuggestionResponseVo response = agentOrchestratorService.reply(user, form);
        return new Result<AgentSuggestionResponseVo>().ok(response);
    }

    @Login
    @PostMapping("/profile/polish")
    @Operation(summary = "AI文案润色")
    public Result<AgentSuggestionResponseVo> profilePolish(
            @Valid @RequestBody AgentProfilePolishForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AgentSuggestionResponseVo response = agentOrchestratorService.polish(user, form);
        return new Result<AgentSuggestionResponseVo>().ok(response);
    }

    @Login
    @PostMapping("/next-step")
    @Operation(summary = "获取AI关系推进策略")
    public Result<AgentNextStepVo> nextStep(
            @Valid @RequestBody AgentNextStepForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AgentNextStepVo response = agentOrchestratorService.nextStep(user, form);
        return new Result<AgentNextStepVo>().ok(response);
    }

    @Login
    @PostMapping("/next-step/actions/execute")
    @Operation(summary = "执行 Agent 下一步结构化动作建议")
    public Result<AgentSuggestedActionExecuteVo> executeSuggestedAction(
            @Valid @RequestBody AgentSuggestedActionExecuteForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentSuggestedActionExecuteVo>().ok(agentSuggestedActionDispatchService.execute(user, form));
    }

    @Login
    @PostMapping("/profile/tag-suggestions")
    @Operation(summary = "AI推荐资料标签（辅助决策）")
    public Result<List<AgentTagSuggestionVO>> profileTagSuggestions(
            @RequestBody(required = false) AgentTagSuggestionForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (!isEnabled("ai_tag_suggest_enabled", true)) {
            return new Result<List<AgentTagSuggestionVO>>().ok(java.util.Collections.emptyList());
        }
        AgentTagSuggestionForm safeForm = form == null ? new AgentTagSuggestionForm() : form;
        List<AgentTagSuggestionVO> response = agentProfileTagSuggestService.suggest(
                user.getUid(),
                safeForm.getTargetUid(),
                safeForm.getLimit()
        );
        return new Result<List<AgentTagSuggestionVO>>().ok(response);
    }

    @Login
    @PostMapping("/video/script")
    @Operation(summary = "AI求爱视频脚本建议")
    public Result<AgentSuggestionResponseVo> videoScript(
            @Valid @RequestBody AgentVideoScriptForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireAiVideoEnabled();
        AgentSuggestionResponseVo response = agentOrchestratorService.videoScript(user, form);
        return new Result<AgentSuggestionResponseVo>().ok(response);
    }

    @Login
    @GetMapping("/gift/catalog")
    @Operation(summary = "获取AI礼物目录配置")
    public Result<AgentGiftPlanVo> giftCatalog(
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        AgentGiftPlanVo response = agentOrchestratorService.giftCatalog(user);
        return new Result<AgentGiftPlanVo>().ok(response);
    }

    @Login
    @PostMapping("/gift/plan")
    @Operation(summary = "获取AI礼物策划")
    public Result<AgentGiftPlanVo> giftPlan(
            @Valid @RequestBody AgentGiftPlanForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        AgentGiftPlanVo response = agentOrchestratorService.giftPlan(user, form);
        return new Result<AgentGiftPlanVo>().ok(response);
    }

    @Login
    @PostMapping("/gift/execute")
    @Operation(summary = "执行礼物打赏并创建礼物任务")
    public Result<AgentGiftExecuteVo> executeGift(
            @Valid @RequestBody AgentGiftExecuteForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        AgentGiftExecuteVo response = agentOrchestratorService.executeGift(user, form);
        return new Result<AgentGiftExecuteVo>().ok(response);
    }

    @Login
    @GetMapping("/gift/task/{taskId}")
    @Operation(summary = "查询礼物任务状态")
    public Result<AgentGiftExecuteVo> getGiftTask(
            @PathVariable("taskId") Long taskId,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireSocialIntentEnabled();
        AgentGiftExecuteVo response = agentOrchestratorService.getGiftTask(user, taskId);
        return new Result<AgentGiftExecuteVo>().ok(response);
    }

    @Login
    @PostMapping("/content/draft")
    @Operation(summary = "Agent 内容草稿：图文动态 / AI视频")
    public Result<AgentContentDraftVo> draftContent(
            @RequestBody(required = false) AgentContentForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AgentContentForm safeForm = form == null ? new AgentContentForm() : form;
        AgentContentDraftVo response = agentContentCreativeService.draft(user, safeForm);
        return new Result<AgentContentDraftVo>().ok(response);
    }

    @Login
    @PostMapping("/content/create")
    @Operation(summary = "Agent 创建内容：直接创建图文动态或AI视频任务")
    public Result<AgentContentDraftVo> createContent(
            @RequestBody(required = false) AgentContentForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        AgentContentForm safeForm = form == null ? new AgentContentForm() : form;
        AgentContentDraftVo response = agentContentCreativeService.create(user, safeForm);
        return new Result<AgentContentDraftVo>().ok(response);
    }

    @Login
    @PostMapping("/relationship/evaluate")
    @Operation(summary = "评估对象关系阶段")
    public Result<AgentRelationshipEvaluateVo> evaluateRelationship(
            @Valid @RequestBody AgentRelationshipEvaluateForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentRelationshipEvaluateVo>().ok(agentAutonomyService.evaluate(user, form));
    }

    @Login
    @PostMapping("/relationship/style/select")
    @Operation(summary = "选择对象当前恋爱大师风格")
    public Result<AgentCoachStyleVo> selectRelationshipStyle(
            @Valid @RequestBody AgentRelationshipEvaluateForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentCoachStyleVo>().ok(agentAutonomyService.selectStyle(user, form));
    }

    @Login
    @PostMapping("/program/run")
    @Operation(summary = "运行对象级自治程序")
    public Result<AgentProgramRunVo> runRelationshipProgram(
            @Valid @RequestBody AgentRelationshipProgramRunForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentProgramRunVo>().ok(agentAutonomyService.runProgram(user, form));
    }

    @Login
    @PostMapping("/feedback")
    @Operation(summary = "Agent建议反馈埋点")
    public Result<Map<String, Object>> feedback(
            @Valid @RequestBody AgentFeedbackForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Map<String, Object> response = agentOrchestratorService.feedback(user, form);
        return new Result<Map<String, Object>>().ok(response);
    }

    @Login
    @GetMapping("/permissions")
    @Operation(summary = "获取智能恋爱助手权限配置")
    public Result<List<AgentPermissionVo>> permissions(
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (!miniAppFilingFeatureService.isAssistantEnabled()) {
            return new Result<List<AgentPermissionVo>>().ok(List.of());
        }
        return new Result<List<AgentPermissionVo>>().ok(agentGovernanceService.listPermissions(user.getUid()));
    }

    @Login
    @GetMapping("/companion/status")
    @Operation(summary = "获取智能恋爱助手状态摘要")
    public Result<AgentCompanionStatusVo> companionStatus(
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (!miniAppFilingFeatureService.isAssistantEnabled()) {
            return new Result<>();
        }
        AgentCompanionStatusVo status = agentAccessPolicyService == null ? null : agentAccessPolicyService.getStatus(user);
        return new Result<AgentCompanionStatusVo>().ok(status);
    }

    @Login
    @PostMapping("/permissions/save")
    @Operation(summary = "保存智能恋爱助手权限配置")
    public Result<AgentPermissionVo> savePermission(
            @Valid @RequestBody AgentPermissionSaveForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (!miniAppFilingFeatureService.isAssistantEnabled()) {
            return new Result<>();
        }
        return new Result<AgentPermissionVo>().ok(agentGovernanceService.savePermission(user.getUid(), form));
    }

    @Login
    @GetMapping("/actions/pending")
    @Operation(summary = "获取待审批动作列表")
    public Result<List<AgentActionTaskVo>> pendingActions(
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<List<AgentActionTaskVo>>().ok(agentGovernanceService.listPendingTasks(user.getUid()));
    }

    @Login
    @GetMapping("/actions/{taskId}")
    @Operation(summary = "获取智能恋爱助手动作详情")
    public Result<AgentActionTaskVo> actionDetail(
            @PathVariable("taskId") Long taskId,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentActionTaskVo>().ok(agentGovernanceService.getTaskDetail(user, taskId));
    }

    @Login
    @GetMapping("/actions/history")
    @Operation(summary = "获取智能恋爱助手动作历史")
    public Result<List<AgentActionTaskVo>> actionHistory(
            @RequestParam(value = "capabilityCode", required = false) String capabilityCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<List<AgentActionTaskVo>>().ok(
                agentGovernanceService.listActionHistory(user.getUid(), capabilityCode, status, limit)
        );
    }

    @Login
    @PostMapping("/actions/{taskId}/approve")
    @Operation(summary = "审批通过智能恋爱助手动作")
    public Result<AgentActionTaskVo> approveAction(
            @PathVariable("taskId") Long taskId,
            @RequestBody(required = false) AgentApprovalDecisionForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        String note = form == null ? "" : form.getNote();
        return new Result<AgentActionTaskVo>().ok(agentGovernanceService.approveTask(user, taskId, note));
    }

    @Login
    @PostMapping("/actions/{taskId}/reject")
    @Operation(summary = "拒绝智能恋爱助手动作")
    public Result<AgentActionTaskVo> rejectAction(
            @PathVariable("taskId") Long taskId,
            @RequestBody(required = false) AgentApprovalDecisionForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        String note = form == null ? "" : form.getNote();
        return new Result<AgentActionTaskVo>().ok(agentGovernanceService.rejectTask(user, taskId, note));
    }

    @Login
    @GetMapping("/actions/{taskId}/logs")
    @Operation(summary = "获取智能恋爱助手动作日志")
    public Result<List<AgentActionLogVo>> actionLogs(
            @PathVariable("taskId") Long taskId,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<List<AgentActionLogVo>>().ok(agentGovernanceService.listTaskLogs(user, taskId));
    }

    @Login
    @PostMapping("/message/auto-send")
    @Operation(summary = "智能恋爱助手自动发送私聊消息")
    public Result<AgentMessageAutoSendVo> autoSendMessage(
            @Valid @RequestBody AgentMessageAutoSendForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        return new Result<AgentMessageAutoSendVo>().ok(agentGovernanceService.executeMessageAutoSendWithGovernance(user, form));
    }

    private boolean isEnabled(String key, boolean defaultValue) {
        try {
            String value = sysConfigService.getValue(key);
            if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
                return defaultValue;
            }
            return !"0".equals(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
