package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aileme.common.satoken.utils.LoginHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.api.service.HongniangMatchCaseService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.HongniangMatchCaseEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/hongniang-match-case")
@Tag(name = "管理端——牵线案件")
public class HongniangMatchCaseController {

    private final HongniangMatchCaseService matchCaseService;

    public HongniangMatchCaseController(HongniangMatchCaseService matchCaseService) {
        this.matchCaseService = matchCaseService;
    }

    @GetMapping("/list")
    @Operation(summary = "牵线案件列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = matchCaseService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "牵线案件详情")
    public R info(@PathVariable("id") Integer id) {
        return R.ok().put("data", matchCaseService.getDetail(id));
    }

    @SysLog("新建牵线案件")
    @PostMapping("/create")
    @Operation(summary = "新建牵线案件")
    public R create(@RequestBody HongniangMatchCaseEntity entity) {
        matchCaseService.createCase(entity, LoginHelper.getUserId());
        return R.ok();
    }

    @SysLog("修改牵线案件")
    @PutMapping("/update")
    @Operation(summary = "修改牵线案件")
    public R update(@RequestBody HongniangMatchCaseEntity entity) {
        matchCaseService.updateCase(entity, LoginHelper.getUserId());
        return R.ok();
    }

    @SysLog("推进牵线案件阶段")
    @PostMapping("/advance-stage")
    @Operation(summary = "推进案件阶段")
    public R advanceStage(@RequestBody Map<String, Object> body) {
        matchCaseService.advanceStage(
            Integer.parseInt(String.valueOf(body.get("caseId"))),
            Integer.parseInt(String.valueOf(body.get("targetStage"))),
            body.get("content") == null ? null : String.valueOf(body.get("content")),
            parseDate(body.get("actualFollowTime")),
            parseDate(body.get("nextFollowTime")),
            body.get("attachments") == null ? null : String.valueOf(body.get("attachments")),
            LoginHelper.getUserId()
        );
        return R.ok();
    }

    @SysLog("新增牵线案件跟进")
    @PostMapping("/add-progress")
    @Operation(summary = "新增跟进记录")
    public R addProgress(@RequestBody Map<String, Object> body) {
        matchCaseService.addProgress(
            Integer.parseInt(String.valueOf(body.get("caseId"))),
            body.get("progressType") == null ? null : Integer.parseInt(String.valueOf(body.get("progressType"))),
            body.get("content") == null ? null : String.valueOf(body.get("content")),
            parseDate(body.get("plannedFollowTime")),
            parseDate(body.get("actualFollowTime")),
            body.get("attachments") == null ? null : String.valueOf(body.get("attachments")),
            LoginHelper.getUserId()
        );
        return R.ok();
    }

    @SysLog("关闭牵线案件")
    @PostMapping("/close")
    @Operation(summary = "关闭牵线案件")
    public R close(@RequestBody Map<String, Object> body) {
        matchCaseService.closeCase(
            Integer.parseInt(String.valueOf(body.get("caseId"))),
            String.valueOf(body.get("closeReason")),
            body.get("content") == null ? null : String.valueOf(body.get("content")),
            LoginHelper.getUserId()
        );
        return R.ok();
    }

    @SysLog("绑定牵线案件微信群")
    @PostMapping("/bind-groups")
    @Operation(summary = "绑定案件微信群")
    public R bindGroups(@RequestBody Map<String, Object> body) {
        Integer caseId = Integer.parseInt(String.valueOf(body.get("caseId")));
        @SuppressWarnings("unchecked")
        List<Integer> groupIds = (List<Integer>) body.get("groupIds");
        matchCaseService.bindGroups(caseId, groupIds);
        return R.ok();
    }

    @GetMapping("/pool-users")
    @Operation(summary = "搜索红娘用户池")
    public R poolUsers(@RequestParam Integer hongniangId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer gender,
                       @RequestParam(required = false) Integer limit,
                       @RequestParam(required = false) Integer excludeUserId) {
        return R.ok().put("list", matchCaseService.searchPoolUsers(hongniangId, keyword, gender, limit, excludeUserId));
    }

    @GetMapping("/source-candidates")
    @Operation(summary = "搜索案件来源候选")
    public R sourceCandidates(@RequestParam Integer hongniangId,
                              @RequestParam Integer sourceType,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Integer limit) {
        return R.ok().put("list", matchCaseService.listSourceCandidates(hongniangId, sourceType, keyword, limit));
    }

    @SysLog("发起牵线申请")
    @PostMapping("/create-request")
    @Operation(summary = "发起牵线申请")
    public R createRequest(@RequestBody Map<String, Object> body) {
        return R.ok().put("data", matchCaseService.createMatchRequest(
            Integer.parseInt(String.valueOf(body.get("caseId"))),
            Integer.parseInt(String.valueOf(body.get("fromUserId"))),
            Integer.parseInt(String.valueOf(body.get("toUserId"))),
            body.get("requestChannel") == null ? null : Integer.parseInt(String.valueOf(body.get("requestChannel"))),
            body.get("requestMessage") == null ? null : String.valueOf(body.get("requestMessage")),
            body.get("wechatShareSnapshot") == null ? null : String.valueOf(body.get("wechatShareSnapshot")),
            parseDate(body.get("expireTime")),
            LoginHelper.getUserId()
        ));
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
        throw new IllegalArgumentException("无法解析日期: " + text);
    }
}
