package org.aileme.shejiao.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility // Removed due to Spring Boot 3.x compatibility issues
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.app.AiBot;
import org.aileme.shejiao.api.service.AiBotService;

import java.util.Arrays;
import java.util.Map;


/**
 * ai模块
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-04 22:14:59
 */
@RestController
@RequestMapping("/admin/aiApp")
@Tag(name = "管理端——ai应用")
public class AiAppController {


    @Autowired
    private AiBotService aiAppService;

    /**
     * 列表
     */
    @GetMapping("/list")
    // @RequiresPermissions("admin:aiApp:list") // Temporarily removed due to Shiro removal
    @Operation(summary = "ai列表")
    public R list(@RequestParam Map<String, Object> params){
        IPage<AiBot> page = aiAppService.page(
                new Query<AiBot>().getPage(params),
                new QueryWrapper<>()
        );
        PageUtils pages =new PageUtils(page);

        return R.ok().put("page", pages);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    // @RequiresPermissions("admin:aiApp:info") // Temporarily removed due to Shiro removal
    @Operation(summary = "ai详情")
    public R info(@PathVariable("id") Integer id){
		AiBot aiApp = aiAppService.getById(id);

        return R.ok().put("aiApp", aiApp);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    // @RequiresPermissions("admin:aiApp:save") // Temporarily removed due to Shiro removal
    @Operation(summary = "ai新增")
    public R save(@RequestBody AiBot aiApp){
        aiApp.setCreateTime(DateUtil.nowDateTime());
        aiApp.setUpdateTime(DateUtil.nowDateTime());
		aiAppService.save(aiApp);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    // @RequiresPermissions("admin:aiApp:update") // Temporarily removed due to Shiro removal
    @Operation(summary = "ai修改")
    public R update(@RequestBody AiBot aiApp){
        aiApp.setUpdateTime(DateUtil.nowDateTime());
		aiAppService.updateById(aiApp);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    // @RequiresPermissions("admin:aiApp:delete") // Temporarily removed due to Shiro removal
    @Operation(summary = "ai删除")
    public R delete(@RequestBody Integer[] ids){
		aiAppService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}