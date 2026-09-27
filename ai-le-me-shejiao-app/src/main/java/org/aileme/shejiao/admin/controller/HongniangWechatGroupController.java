package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.aileme.shejiao.api.service.HongniangWechatGroupService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.HongniangGroupTouchTaskEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangWechatGroupEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/hongniang-wechat-group")
@Tag(name = "管理端——微信群管理")
public class HongniangWechatGroupController {

    private final HongniangWechatGroupService wechatGroupService;

    public HongniangWechatGroupController(HongniangWechatGroupService wechatGroupService) {
        this.wechatGroupService = wechatGroupService;
    }

    @GetMapping("/list")
    @Operation(summary = "微信群列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = wechatGroupService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "微信群详情")
    public R info(@PathVariable("id") Integer id) {
        return R.ok().put("data", wechatGroupService.getDetail(id));
    }

    @SysLog("新建微信群资产")
    @PostMapping("/create")
    @Operation(summary = "新建微信群")
    public R create(@RequestBody HongniangWechatGroupEntity entity) {
        wechatGroupService.saveGroup(entity);
        return R.ok();
    }

    @SysLog("修改微信群资产")
    @PutMapping("/update")
    @Operation(summary = "修改微信群")
    public R update(@RequestBody HongniangWechatGroupEntity entity) {
        wechatGroupService.updateGroup(entity);
        return R.ok();
    }

    @SysLog("绑定微信群用户")
    @PostMapping("/bind-users")
    @Operation(summary = "绑定群用户")
    public R bindUsers(@RequestBody Map<String, Object> body) {
        Integer groupId = Integer.parseInt(String.valueOf(body.get("groupId")));
        @SuppressWarnings("unchecked")
        List<Integer> userIds = (List<Integer>) body.get("userIds");
        wechatGroupService.bindUsers(groupId, userIds);
        return R.ok();
    }

    @SysLog("绑定微信群案件")
    @PostMapping("/bind-cases")
    @Operation(summary = "绑定群案件")
    public R bindCases(@RequestBody Map<String, Object> body) {
        Integer groupId = Integer.parseInt(String.valueOf(body.get("groupId")));
        @SuppressWarnings("unchecked")
        List<Integer> caseIds = (List<Integer>) body.get("caseIds");
        wechatGroupService.bindCases(groupId, caseIds);
        return R.ok();
    }

    @SysLog("同步微信群")
    @PostMapping("/sync")
    @Operation(summary = "同步群信息")
    public R sync(@RequestBody Map<String, Object> body) {
        Integer groupId = Integer.parseInt(String.valueOf(body.get("groupId")));
        return R.ok().put("data", wechatGroupService.syncGroup(groupId));
    }

    @SysLog("创建群触达任务")
    @PostMapping("/touch-task/create")
    @Operation(summary = "创建群触达任务")
    public R createTouchTask(@RequestBody HongniangGroupTouchTaskEntity entity) {
        return R.ok().put("data", wechatGroupService.createTouchTask(entity));
    }

    @GetMapping("/touch-task/list")
    @Operation(summary = "群触达任务列表")
    public R listTouchTask(@RequestParam Map<String, Object> params) {
        return R.ok().put("page", wechatGroupService.listTouchTasks(params));
    }

    @SysLog("重试群触达任务")
    @PostMapping("/touch-task/retry")
    @Operation(summary = "重试群触达任务")
    public R retryTouchTask(@RequestBody Map<String, Object> body) {
        Integer taskId = Integer.parseInt(String.valueOf(body.get("taskId")));
        return R.ok().put("data", wechatGroupService.retryTouchTask(taskId));
    }

    @GetMapping("/touch-log/list")
    @Operation(summary = "群触达日志列表")
    public R listTouchLog(@RequestParam Map<String, Object> params) {
        return R.ok().put("page", wechatGroupService.listTouchLogs(params));
    }

    @GetMapping("/touch-execution/list")
    @Operation(summary = "群任务执行记录")
    public R listTouchExecution(@RequestParam(required = false) Integer taskId,
                                @RequestParam(required = false) Integer groupId,
                                @RequestParam(required = false) Integer limit) {
        return R.ok().put("list", wechatGroupService.listTaskExecutions(taskId, groupId, limit));
    }

    @GetMapping("/bindable-users")
    @Operation(summary = "搜索可绑定群用户")
    public R bindableUsers(@RequestParam Integer hongniangId,
                           @RequestParam(required = false) Integer groupId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer limit) {
        return R.ok().put("list", wechatGroupService.searchBindableUsers(groupId, hongniangId, keyword, limit));
    }

    @GetMapping("/bindable-cases")
    @Operation(summary = "搜索可绑定群案件")
    public R bindableCases(@RequestParam Integer hongniangId,
                           @RequestParam(required = false) Integer groupId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer limit) {
        return R.ok().put("list", wechatGroupService.searchBindableCases(groupId, hongniangId, keyword, limit));
    }
}
