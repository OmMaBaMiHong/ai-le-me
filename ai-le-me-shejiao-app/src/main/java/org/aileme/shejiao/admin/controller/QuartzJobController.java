package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.SysQuartzJobService;
import org.aileme.shejiao.app.service.quartz.QuartzTaskHandlerRegistry;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.job.SysQuartzJob;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/admin/quartzJob")
@Tag(name = "管理端——Quartz任务管理")
public class QuartzJobController {

    private final SysQuartzJobService quartzJobService;
    private final QuartzTaskHandlerRegistry quartzTaskHandlerRegistry;

    public QuartzJobController(SysQuartzJobService quartzJobService,
                               QuartzTaskHandlerRegistry quartzTaskHandlerRegistry) {
        this.quartzJobService = quartzJobService;
        this.quartzTaskHandlerRegistry = quartzTaskHandlerRegistry;
    }

    @GetMapping("/list")
    @Operation(summary = "任务列表")
    public R list(@RequestParam Map<String, Object> params) {
        return R.ok().put("page", quartzJobService.queryPage(params));
    }

    @GetMapping("/logs")
    @Operation(summary = "任务日志列表")
    public R logs(@RequestParam Map<String, Object> params) {
        return R.ok().put("page", quartzJobService.queryLogPage(params));
    }

    @GetMapping("/handlers")
    @Operation(summary = "任务处理器列表")
    public R handlers() {
        return R.ok().put("list", quartzTaskHandlerRegistry.listHandlers());
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "任务详情")
    public R info(@PathVariable("id") Long id) {
        return R.ok().put("job", quartzJobService.getDetail(id));
    }

    @SysLog("新增Quartz任务")
    @PostMapping("/save")
    @Operation(summary = "新增任务")
    public R save(@RequestBody SysQuartzJob job) {
        quartzJobService.saveJob(job);
        return R.ok();
    }

    @SysLog("修改Quartz任务")
    @PostMapping("/update")
    @Operation(summary = "修改任务")
    public R update(@RequestBody SysQuartzJob job) {
        quartzJobService.updateJob(job);
        return R.ok();
    }

    @SysLog("删除Quartz任务")
    @PostMapping("/delete")
    @Operation(summary = "删除任务")
    public R delete(@RequestBody Long[] ids) {
        quartzJobService.deleteJobs(Arrays.asList(ids));
        return R.ok();
    }

    @SysLog("切换Quartz任务状态")
    @PostMapping("/changeStatus/{id}/{status}")
    @Operation(summary = "切换任务状态")
    public R changeStatus(@PathVariable("id") Long id, @PathVariable("status") Integer status) {
        quartzJobService.changeStatus(id, status);
        return R.ok();
    }

    @SysLog("立即执行Quartz任务")
    @PostMapping("/runOnce/{id}")
    @Operation(summary = "立即执行一次")
    public R runOnce(@PathVariable("id") Long id) {
        quartzJobService.runOnce(id);
        return R.ok();
    }
}
