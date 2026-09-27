package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.HongniangApplyService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.HongniangApplyEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;

/**
 * App端-红娘申请Controller
 */
@RestController
@RequestMapping("/app/hongniang/apply")
@Tag(name = "App端——红娘申请")
public class AppHongniangApplyController {

    @Autowired
    private HongniangApplyService applyService;

    @Autowired
    private HongniangService hongniangService;

    @GetMapping("/checkStatus")
    @Operation(summary = "检查申请状态")
    public R checkStatus(@RequestParam Integer userId) {
        HongniangApplyEntity apply = applyService.getLatestByUserId(userId);
        HongniangInfoEntity hongniang = hongniangService.getByUserId(userId);
        return R.ok().put("apply", apply).put("hongniang", hongniang);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交申请")
    public R submit(@RequestBody HongniangApplyEntity apply) {
        applyService.submitApply(apply);
        return R.ok().put("msg", "申请提交成功，我们将在1-3个工作日内完成审核");
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "申请详情")
    public R detail(@PathVariable("id") Long id) {
        HongniangApplyEntity apply = applyService.getById(id);
        return R.ok().put("apply", apply);
    }
}
