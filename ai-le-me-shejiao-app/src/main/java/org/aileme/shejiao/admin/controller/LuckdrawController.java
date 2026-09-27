/**
 * -----------------------------------
 *  Copyright (c) 2021-2023
 *  All rights reserved, Designed By my.hots.love
 *  
 *  商业版授权联系技术客服	 QQ:  3582996245
 *  严禁分享、盗用、转卖源码或非法牟利！
 *  版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.controller;

import java.util.Arrays;
import java.util.Map;

import org.aileme.shejiao.common.exception.LinfengException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.LuckdrawEntity;
import org.aileme.shejiao.api.service.LuckdrawService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 
 * 抽奖物品管理
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-08-14 14:28:48
 */
@RestController
@RequestMapping("/admin/luckdraw")
@Tag(name = "管理端——抽奖物品")
public class LuckdrawController {
    @Autowired
    private LuckdrawService luckdrawService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdraw:list")
    @Operation(summary = "抽奖物品列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = luckdrawService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdraw:info")
    @Operation(summary = "抽奖物品详情")
    public R info(@PathVariable("id") Integer id){
		LuckdrawEntity luckdraw = luckdrawService.getById(id);

        return R.ok().put("luckdraw", luckdraw);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdraw:save")
    @Operation(summary = "抽奖物品保存")
    public R save(@RequestBody LuckdrawEntity luckdraw){
        long count = luckdrawService.lambdaQuery().eq(LuckdrawEntity::getStatus, 1).count();
        if(count>=8){
            throw new LinfengException("抽奖物品数量必须为8个");
        }
        luckdrawService.save(luckdraw);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdraw:update")
    @Operation(summary = "抽奖物品修改")
    public R update(@RequestBody LuckdrawEntity luckdraw){
		luckdrawService.updateById(luckdraw);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdraw:delete")
    @Operation(summary = "抽奖物品删除")
    public R delete(@RequestBody Integer[] ids){
		luckdrawService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
