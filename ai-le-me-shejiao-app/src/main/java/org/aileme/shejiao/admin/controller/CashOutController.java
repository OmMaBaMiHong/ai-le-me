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

/* // // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.aileme.shejiao.common.annotation.NoRepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.CashOutEntity;
import org.aileme.shejiao.api.service.CashOutService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 提现
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-01 11:43:29
 */
@RestController
@RequestMapping("/admin/cashout")
@Tag(name = "管理端——提现")
public class CashOutController {
    @Autowired
    private CashOutService cashOutService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:cashout:list")
    @Operation(summary = "提现列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = cashOutService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:cashout:info")
    @Operation(summary = "提现详情")
    public R info(@PathVariable("id") Integer id){
		CashOutEntity cashOut = cashOutService.getById(id);

        return R.ok().put("cashOut", cashOut);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:cashout:save")
    @Operation(summary = "提现保存")
    public R save(@RequestBody CashOutEntity cashOut){
		cashOutService.save(cashOut);

        return R.ok();
    }

    /**
     * 修改
     */
    @NoRepeatSubmit
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:cashout:update")
    @Operation(summary = "提现修改")
    public R update(@RequestBody CashOutEntity cashOut){

		cashOutService.updateCash(cashOut);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:cashout:delete")
    @Operation(summary = "提现删除")
    public R delete(@RequestBody Integer[] ids){
		cashOutService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
