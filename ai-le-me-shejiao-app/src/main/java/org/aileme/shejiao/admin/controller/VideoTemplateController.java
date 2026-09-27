package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.VideoTemplateService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.app.VideoTemplateEntity;

import java.util.Arrays;
import java.util.Map;

/**
 * 管理端 - AI 视频模板管理
 */
@RestController
@RequestMapping("/admin/videoTemplate")
@Tag(name = "管理端——AI视频模板管理")
public class VideoTemplateController {

    @Autowired
    private VideoTemplateService videoTemplateService;

    @GetMapping("/list")
    @Operation(summary = "视频模板列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = videoTemplateService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "视频模板详情")
    public R info(@PathVariable("id") Integer id) {
        VideoTemplateEntity template = videoTemplateService.getById(id);
        return R.ok().put("template", template);
    }

    @SysLog("新增AI视频模板")
    @PostMapping("/save")
    @Operation(summary = "新增视频模板")
    public R save(@RequestBody VideoTemplateEntity template) {
        videoTemplateService.save(template);
        return R.ok();
    }

    @SysLog("修改AI视频模板")
    @PostMapping("/update")
    @Operation(summary = "修改视频模板")
    public R update(@RequestBody VideoTemplateEntity template) {
        videoTemplateService.updateById(template);
        return R.ok();
    }

    @SysLog("删除AI视频模板")
    @PostMapping("/delete")
    @Operation(summary = "删除视频模板")
    public R delete(@RequestBody Integer[] ids) {
        videoTemplateService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }
}
