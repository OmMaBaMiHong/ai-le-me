/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 *
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
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

import org.aileme.shejiao.domain.entity.admin.ReportEntity;
import org.aileme.shejiao.api.service.ReportService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 用户举报
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-09-01 12:55:12
 */
@RestController
@RequestMapping("/admin/report")
@Tag(name = "管理端——用户举报管理")
public class ReportController {
    @Autowired
    private ReportService reportService;



    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:report:list")
    @Operation(summary = "用户举报列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = reportService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:report:info")
    @Operation(summary = "用户举报详情")
    public R info(@PathVariable("id") Integer id){
		ReportEntity report = reportService.getById(id);

        return R.ok().put("report", report);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:report:save")
    @Operation(summary = "用户举报保存")
    public R save(@RequestBody ReportEntity report){
		reportService.save(report);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:report:update")
    @Operation(summary = "用户举报修改")
    public R update(@RequestBody ReportEntity report){
//		reportService.updateById(report);
        reportService.dealByAdmin(report);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:report:delete")
    @Operation(summary = "用户举报删除")
    public R delete(@RequestBody Integer[] ids){
		reportService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
