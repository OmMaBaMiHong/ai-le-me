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

import org.aileme.shejiao.common.utils.ConfigConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.UserMenuEntity;
import org.aileme.shejiao.api.service.UserMenuService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 用户菜单
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-07-22 09:33:30
 */
@RestController
@RequestMapping("/admin/usermenu")
@Tag(name = "管理端——用户菜单管理")
public class UserMenuController {

    @Autowired
    private UserMenuService userMenuService;
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usermenu:list")
    @Operation(summary = "用户菜单列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = userMenuService.queryPage(params);

        return R.ok().put("page", page);
    }




    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usermenu:info")
    @Operation(summary = "用户菜单详情")
    public R info(@PathVariable("id") Integer id){
		UserMenuEntity userMenu = userMenuService.getById(id);

        return R.ok().put("userMenu", userMenu);
    }



    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usermenu:save")
    @Operation(summary = "用户菜单保存")
    public R save(@RequestBody UserMenuEntity userMenu){
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.USER_MENU_CONFIG_KEY);
		userMenuService.save(userMenu);

        return R.ok();
    }



    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usermenu:update")
    @Operation(summary = "用户菜单修改")
    public R update(@RequestBody UserMenuEntity userMenu){
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.USER_MENU_CONFIG_KEY);
		userMenuService.updateById(userMenu);

        return R.ok();
    }



    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usermenu:delete")
    @Operation(summary = "用户菜单删除")
    public R delete(@RequestBody Integer[] ids){
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.USER_MENU_CONFIG_KEY);
		userMenuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
