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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.LuckdrawRecordEntity;
import org.aileme.shejiao.api.service.LuckdrawRecordService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 
 * 抽奖记录管理
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-08-14 14:28:48
 */
@RestController
@RequestMapping("/admin/luckdrawrecord")
@Tag(name = "管理端——抽奖记录")
public class LuckdrawRecordController {
    @Autowired
    private LuckdrawRecordService luckdrawRecordService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdrawrecord:list")
    @Operation(summary = "抽奖记录列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = luckdrawRecordService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdrawrecord:info")
    @Operation(summary = "抽奖记录详情")
    public R info(@PathVariable("id") Integer id){
		LuckdrawRecordEntity luckdrawRecord = luckdrawRecordService.getById(id);

        return R.ok().put("luckdrawRecord", luckdrawRecord);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdrawrecord:save")
    @Operation(summary = "抽奖记录保存")
    public R save(@RequestBody LuckdrawRecordEntity luckdrawRecord){
		luckdrawRecordService.save(luckdrawRecord);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdrawrecord:update")
    @Operation(summary = "抽奖记录修改")
    public R update(@RequestBody LuckdrawRecordEntity luckdrawRecord){
		luckdrawRecordService.updateById(luckdrawRecord);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:luckdrawrecord:delete")
    @Operation(summary = "抽奖记录删除")
    public R delete(@RequestBody Integer[] ids){
		luckdrawRecordService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
