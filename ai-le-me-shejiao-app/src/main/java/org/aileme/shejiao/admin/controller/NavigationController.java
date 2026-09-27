package org.aileme.shejiao.admin.controller;

import java.util.Arrays;
import java.util.Map;

import org.aileme.shejiao.common.utils.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.NavigationEntity;
import org.aileme.shejiao.api.service.NavigationService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 导航栏模块
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-04 22:14:59
 */
@RestController
@RequestMapping("/admin/navigation")
@Tag(name = "管理端——导航栏管理")
public class NavigationController {


    @Autowired
    private NavigationService navigationService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:navigation:list")
    @Operation(summary = "导航栏列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = navigationService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:navigation:info")
    @Operation(summary = "导航栏详情")
    public R info(@PathVariable("id") Integer id){
		NavigationEntity navigation = navigationService.getById(id);

        return R.ok().put("navigation", navigation);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:navigation:save")
    @Operation(summary = "导航栏新增")
    public R save(@RequestBody NavigationEntity navigation){
        navigation.setCreateTime(DateUtil.nowDateTime());
        navigation.setUpdateTime(DateUtil.nowDateTime());
		navigationService.save(navigation);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:navigation:update")
    @Operation(summary = "导航栏修改")
    public R update(@RequestBody NavigationEntity navigation){
        navigation.setUpdateTime(DateUtil.nowDateTime());
		navigationService.updateById(navigation);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:navigation:delete")
    @Operation(summary = "导航栏删除")
    public R delete(@RequestBody Integer[] ids){
		navigationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
