package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.api.service.XiangqinEnrollmentService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/xiangqin")
@Tag(name = "管理端——相亲局管理")
public class XiangqinActivityController {

    @Autowired
    private XiangqinActivityService activityService;

    @Autowired
    private XiangqinEnrollmentService enrollmentService;

    @GetMapping("/activity/list")
    @Operation(summary = "活动列表")
    public R activityList(@RequestParam Map<String, Object> params) {
        PageUtils page = activityService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/activity/info/{id}")
    @Operation(summary = "活动详情")
    public R activityInfo(@PathVariable("id") Integer id) {
        XiangqinActivityEntity activity = activityService.getById(id);
        return R.ok().put("activity", activity);
    }

    @SysLog("创建相亲局")
    @PostMapping("/activity/save")
    @Operation(summary = "创建活动")
    public R activitySave(@RequestBody XiangqinActivityEntity activity) {
        activityService.saveActivity(activity);
        return R.ok();
    }

    @SysLog("修改相亲局")
    @PostMapping("/activity/update")
    @Operation(summary = "修改活动")
    public R activityUpdate(@RequestBody XiangqinActivityEntity activity) {
        activityService.updateActivity(activity);
        return R.ok();
    }

    @SysLog("删除相亲局")
    @PostMapping("/activity/delete")
    @Operation(summary = "删除活动")
    public R activityDelete(@RequestBody Integer[] ids) {
        activityService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }

    @GetMapping("/enrollment/list")
    @Operation(summary = "报名列表")
    public R enrollmentList(@RequestParam Map<String, Object> params) {
        PageUtils page = enrollmentService.queryPage(params);
        return R.ok().put("page", page);
    }

    @SysLog("审核报名")
    @PostMapping("/enrollment/audit")
    @Operation(summary = "审核报名")
    public R enrollmentAudit(@RequestParam Integer enrollmentId,
                             @RequestParam Integer status,
                             @RequestParam(required = false) String auditRemark) {
        enrollmentService.auditEnroll(enrollmentId, status, auditRemark);
        return R.ok();
    }
}
