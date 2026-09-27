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

import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.ConfigConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.SensitiveEntity;
import org.aileme.shejiao.api.service.SensitiveService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 敏感词库
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-28 13:40:57
 */
@RestController
@RequestMapping("/admin/sensitive")
@Tag(name = "管理端——敏感词库管理")
public class SensitiveController {

    @Autowired
    private SensitiveService sensitiveService;
    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:sensitive:list")
    @Operation(summary = "敏感词库列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = sensitiveService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:sensitive:info")
    @Operation(summary = "敏感词库详情")
    public R info(@PathVariable("id") Long id){
		SensitiveEntity sensitive = sensitiveService.getById(id);

        return R.ok().put("sensitive", sensitive);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:sensitive:save")
    @Operation(summary = "敏感词库新增")
    public R save(@RequestBody SensitiveEntity sensitive){
		sensitiveService.save(sensitive);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.SENSETIVE_LIST_KEY);
        return R.ok();
    }

    /**
     * 修改
     */
    @SysLog("修改敏感词库")
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:sensitive:update")
    @Operation(summary = "修改敏感词库")
    public R update(@RequestBody SensitiveEntity sensitive){
		sensitiveService.updateById(sensitive);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.SENSETIVE_LIST_KEY);
        return R.ok();
    }

    /**
     * 删除
     */
    @SysLog("删除敏感词库")
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:sensitive:delete")
    @Operation(summary = "删除敏感词库")
    public R delete(@RequestBody Long[] ids){
		sensitiveService.removeByIds(Arrays.asList(ids));
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.SENSETIVE_LIST_KEY);
        return R.ok();
    }

}
