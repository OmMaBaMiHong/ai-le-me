package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;

import java.util.Arrays;
import java.util.Map;

/**
 * 红娘管理Controller
 *
 * @author system
 * @date 2026-01-27
 */
@RestController
@RequestMapping("/admin/hongniang")
@Tag(name = "管理端——红娘管理")
public class HongniangController {

    @Autowired
    private HongniangService hongniangService;

    /**
     * 红娘列表
     */
    @GetMapping("/list")
    @Operation(summary = "红娘列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = hongniangService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 红娘详情
     */
    @GetMapping("/info/{id}")
    @Operation(summary = "红娘详情")
    public R info(@PathVariable("id") Long id) {
        HongniangInfoEntity hongniang = hongniangService.getById(id);
        return R.ok().put("hongniang", hongniang);
    }

    /**
     * 保存红娘
     */
    @SysLog("新增红娘")
    @PostMapping("/save")
    @Operation(summary = "保存红娘")
    public R save(@RequestBody HongniangInfoEntity hongniang) {
        hongniangService.saveHongniang(hongniang);
        return R.ok();
    }

    /**
     * 修改红娘
     */
    @SysLog("修改红娘")
    @PostMapping("/update")
    @Operation(summary = "修改红娘")
    public R update(@RequestBody HongniangInfoEntity hongniang) {
        hongniangService.updateHongniang(hongniang);
        return R.ok();
    }

    /**
     * 删除红娘
     */
    @SysLog("删除红娘")
    @PostMapping("/delete")
    @Operation(summary = "删除红娘")
    public R delete(@RequestBody Long[] ids) {
        hongniangService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }

    /**
     * 文件导入用户
     */
    @SysLog("红娘导入用户")
    @PostMapping("/importUsers")
    @Operation(summary = "导入用户（PDF/DOCX）")
    public R importUsers(@RequestParam("file") MultipartFile file,
                        @RequestParam("hongniangId") Integer hongniangId) {
        try {
            int count = hongniangService.importUsersFromFile(file, hongniangId);
            return R.ok().put("count", count).put("msg", "成功导入" + count + "个用户");
        } catch (Exception e) {
            return R.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 更新统计数据
     */
    @PostMapping("/updateStatistics/{id}")
    @Operation(summary = "更新统计数据")
    public R updateStatistics(@PathVariable("id") Integer id) {
        hongniangService.updateStatistics(id);
        return R.ok();
    }
}
