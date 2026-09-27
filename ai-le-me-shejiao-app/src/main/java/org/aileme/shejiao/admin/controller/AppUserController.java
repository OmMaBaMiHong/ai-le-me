/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *
 *  商业版授权联系技术客服	 vx: lwwmmzh
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.controller;

import java.util.Arrays;
import java.util.Map;

import org.aileme.shejiao.common.annotation.NoRepeatSubmit;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.domain.param.app.AdminUserInfoForm;
import org.aileme.shejiao.domain.param.app.AdminUserPunishForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 *
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-20 12:10:43
 */
@RestController
@RequestMapping("/admin/user")
@Tag(name = "管理端——用户管理")
public class AppUserController {
    @Autowired
    private AppUserService appUserService;
    /**
     * 列表
     */
    @GetMapping("/list")
    // @RequiresPermissions("admin:user:list")
    @Operation(summary = "用户列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = appUserService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{uid}")
    // @RequiresPermissions("admin:user:info")
    @Operation(summary = "用户详情")
    public R info(@PathVariable("uid") Integer uid){
		AppUserEntity user = appUserService.getById(uid);

        return R.ok().put("user", user);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    // @RequiresPermissions("admin:user:save")
    @Operation(summary = "用户保存")
    public R save(@RequestBody AppUserEntity user){
		appUserService.save(user);

        return R.ok();
    }

    /**
     * 修改
     */
    @SysLog("修改会员信息")
    @NoRepeatSubmit
    @PostMapping("/update")
    // @RequiresPermissions("admin:user:update")
    @Operation(summary = "修改会员信息")
    public R update(@RequestBody AdminUserInfoForm user){
		appUserService.updateUser(user);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" +user.getUid());
        return R.ok();
    }

    /**
     * 处罚
     */
    @SysLog("处罚用户账号")
    @NoRepeatSubmit
    @PostMapping("/punish")
    // @RequiresPermissions("admin:user:update")
    @Operation(summary = "处罚用户账号")
    public R punish(@RequestBody AdminUserPunishForm param){
        appUserService.punishUser(param);
        org.aileme.common.redis.utils.RedisUtils.deleteObject("userId:" +param.getUid());
        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    // @RequiresPermissions("admin:user:delete")
    @Operation(summary = "用户删除")
    public R delete(@RequestBody Integer[] uids){
		appUserService.removeByIds(Arrays.asList(uids));

        return R.ok();
    }


    @SysLog("禁用会员")
    @PostMapping("/ban/{id}")
    // @RequiresPermissions("admin:user:update")
    @Operation(summary = "禁用会员")
    public R ban(@PathVariable("id") Integer id){
		appUserService.ban(id);

        return R.ok();
    }


    @SysLog("解除会员禁用")
    @PostMapping("/openBan/{id}")
    // @RequiresPermissions("admin:user:update")
    @Operation(summary = "解除会员禁用")
    public R openBan(@PathVariable("id") Integer id){
		appUserService.openBan(id);

        return R.ok();
    }

}
