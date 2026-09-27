package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.api.service.HongniangMatchCaseService;
import org.aileme.shejiao.api.service.HongniangPrivateDomainService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Tag(name = "移动端——红娘工作台")
@RestController
@RequestMapping("/app/hongniang/workbench")
public class AppHongniangWorkbenchController {

    private final HongniangPrivateDomainService privateDomainService;
    private final HongniangService hongniangService;
    private final HongniangMatchCaseService matchCaseService;

    public AppHongniangWorkbenchController(HongniangPrivateDomainService privateDomainService,
                                           HongniangService hongniangService,
                                           HongniangMatchCaseService matchCaseService) {
        this.privateDomainService = privateDomainService;
        this.hongniangService = hongniangService;
        this.matchCaseService = matchCaseService;
    }

    @Login
    @GetMapping("/overview")
    @Operation(summary = "工作台概览")
    public R overview(@Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer hongniangId = resolveHongniangId(user);
        return R.ok().put("result", privateDomainService.workbenchOverview(hongniangId));
    }

    @Login
    @GetMapping("/users")
    @Operation(summary = "工作台用户池")
    public R users(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                   @RequestParam Map<String, Object> params) {
        Integer hongniangId = resolveHongniangId(user);
        return R.ok().put("result", privateDomainService.workbenchUsers(hongniangId, params));
    }

    @Login
    @GetMapping("/cases")
    @Operation(summary = "工作台牵线案件")
    public R cases(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                   @RequestParam Map<String, Object> params) {
        Integer hongniangId = resolveHongniangId(user);
        return R.ok().put("result", privateDomainService.workbenchCases(hongniangId, params));
    }

    @Login
    @GetMapping("/caseDetail")
    @Operation(summary = "工作台案件详情")
    public R caseDetail(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                        @RequestParam Integer caseId) {
        Integer hongniangId = resolveHongniangId(user);
        return R.ok().put("result", privateDomainService.workbenchCaseDetail(hongniangId, caseId));
    }

    @Login
    @PostMapping("/caseProgress")
    @Operation(summary = "工作台新增案件跟进")
    public R addCaseProgress(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                             @RequestBody Map<String, Object> body) {
        Integer hongniangId = resolveHongniangId(user);
        Integer caseId = Integer.parseInt(String.valueOf(body.get("caseId")));
        privateDomainService.workbenchCaseDetail(hongniangId, caseId);
        matchCaseService.addProgress(
            caseId,
            body.get("progressType") == null ? null : Integer.parseInt(String.valueOf(body.get("progressType"))),
            body.get("content") == null ? null : String.valueOf(body.get("content")),
            parseDate(body.get("plannedFollowTime")),
            parseDate(body.get("actualFollowTime")),
            body.get("attachments") == null ? null : String.valueOf(body.get("attachments")),
            Long.valueOf(user.getUid())
        );
        return R.ok();
    }

    @Login
    @PostMapping("/createMatchRequest")
    @Operation(summary = "工作台发起牵线申请")
    public R createMatchRequest(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                                @RequestBody Map<String, Object> body) {
        Integer hongniangId = resolveHongniangId(user);
        Integer caseId = Integer.parseInt(String.valueOf(body.get("caseId")));
        privateDomainService.workbenchCaseDetail(hongniangId, caseId);
        return R.ok().put("result", matchCaseService.createMatchRequest(
            caseId,
            Integer.parseInt(String.valueOf(body.get("fromUserId"))),
            Integer.parseInt(String.valueOf(body.get("toUserId"))),
            body.get("requestChannel") == null ? null : Integer.parseInt(String.valueOf(body.get("requestChannel"))),
            body.get("requestMessage") == null ? null : String.valueOf(body.get("requestMessage")),
            body.get("wechatShareSnapshot") == null ? null : String.valueOf(body.get("wechatShareSnapshot")),
            parseDate(body.get("expireTime")),
            Long.valueOf(user.getUid())
        ));
    }

    @Login
    @GetMapping("/groups")
    @Operation(summary = "工作台微信群")
    public R groups(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                    @RequestParam Map<String, Object> params) {
        Integer hongniangId = resolveHongniangId(user);
        return R.ok().put("result", privateDomainService.workbenchGroups(hongniangId, params));
    }

    @Login
    @GetMapping("/groupDetail")
    @Operation(summary = "工作台微信群详情")
    public R groupDetail(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                         @RequestParam Integer groupId) {
        Integer hongniangId = resolveHongniangId(user);
        return R.ok().put("result", privateDomainService.workbenchGroupDetail(hongniangId, groupId));
    }

    private Integer resolveHongniangId(AppUserEntity user) {
        if (user == null || user.getUid() == null) {
            throw new LinfengException("当前用户不存在");
        }
        if (user.getHongniangId() != null && user.getHongniangId() > 0) {
            return user.getHongniangId();
        }
        var hongniang = hongniangService.getByUserId(user.getUid());
        if (hongniang == null || hongniang.getId() == null) {
            throw new LinfengException("当前账号未绑定红娘身份");
        }
        return hongniang.getId();
    }

    private Date parseDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (ParseException ignored) {
            }
        }
        throw new LinfengException("无法解析日期: " + text);
    }
}
