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
// // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.BillEntity;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 用户账单
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-20 20:46:48
 */
@RestController
@RequestMapping("/admin/bill")
@Tag(name = "管理端——用户账单")
public class BillController {
    @Autowired
    private BillService billService;

    /**
     * 列表
     */
    @GetMapping("/list")
    // @RequiresPermissions("admin:bill:list") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "用户账单列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = billService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    // @RequiresPermissions("admin:bill:info") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "用户账单详情")
    public R info(@PathVariable("id") Integer id){
		BillEntity bill = billService.getById(id);

        return R.ok().put("bill", bill);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    // @RequiresPermissions("admin:bill:save") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "用户账单保存")
    public R save(@RequestBody BillEntity bill){
		billService.save(bill);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    // @RequiresPermissions("admin:bill:update") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "用户账单修改")
    public R update(@RequestBody BillEntity bill){
		billService.updateById(bill);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    // @RequiresPermissions("admin:bill:delete") // Temporarily removed due to Spring Boot 3.x compatibility
    @Operation(summary = "用户账单删除")
    public R delete(@RequestBody Integer[] ids){
		billService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
