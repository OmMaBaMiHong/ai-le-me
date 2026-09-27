/**
 * -----------------------------------
 *  Copyright (c) 2021-2024
 *  All rights reserved, Designed By www.linfengtech.cn
 *  林风社交论坛商业版本请务必保留此注释头信息
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.controller;

import java.util.Arrays;
import java.util.Map;

import org.aileme.shejiao.common.annotation.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.SignConfigEntity;
import org.aileme.shejiao.api.service.SignConfigService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 20:28:46
 */
@RestController
@RequestMapping("/admin/signconfig")
@Tag(name = "管理端——签到配置管理")
public class SignConfigController {
    @Autowired
    private SignConfigService signConfigService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
//
//    "admin:signconfig:list")
    @Operation(summary = "签到配置列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = signConfigService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
//
//    "admin:signconfig:info")
    @Operation(summary = "签到配置详情")
    public R info(@PathVariable("id") Integer id){
		SignConfigEntity signConfig = signConfigService.getById(id);

        return R.ok().put("signConfig", signConfig);
    }

    /**
     * 保存
     */
    @SysLog("保存签到配置")
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
//
//    "admin:signconfig:save")
    @Operation(summary = "签到配置保存")
    public R save(@RequestBody SignConfigEntity signConfig){
		signConfigService.save(signConfig);

        return R.ok();
    }

    /**
     * 修改
     */
    @SysLog("修改签到配置")
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
//
//    "admin:signconfig:update")
    @Operation(summary = "签到配置修改")
    public R update(@RequestBody SignConfigEntity signConfig){
		signConfigService.updateById(signConfig);

        return R.ok();
    }

    /**
     * 删除
     */
    @SysLog("删除签到配置")
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
//
//    "admin:signconfig:delete")
    @Operation(summary = "签到配置删除")
    public R delete(@RequestBody Integer[] ids){
		signConfigService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
