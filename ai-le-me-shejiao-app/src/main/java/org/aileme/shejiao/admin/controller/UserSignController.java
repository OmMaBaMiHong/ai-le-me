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

import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.UserSignEntity;
import org.aileme.shejiao.api.service.UserSignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * 签到记录
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 15:01:03
 */
@RestController
@RequestMapping("/admin/usersign")
@Tag(name = "管理端——签到记录")
public class UserSignController {
    @Autowired
    private UserSignService userSignService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usersign:list")
    @Operation(summary = "签到记录列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = userSignService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usersign:info")
    @Operation(summary = "签到记录详情")
    public R info(@PathVariable("id") Integer id){
		UserSignEntity userSign = userSignService.getById(id);

        return R.ok().put("userSign", userSign);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usersign:save")
    @Operation(summary = "签到记录保存")
    public R save(@RequestBody UserSignEntity userSign){
		userSignService.save(userSign);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usersign:update")
    @Operation(summary = "签到记录修改")
    public R update(@RequestBody UserSignEntity userSign){
		userSignService.updateById(userSign);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:usersign:delete")
    @Operation(summary = "签到记录删除")
    public R delete(@RequestBody Integer[] ids){
		userSignService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
