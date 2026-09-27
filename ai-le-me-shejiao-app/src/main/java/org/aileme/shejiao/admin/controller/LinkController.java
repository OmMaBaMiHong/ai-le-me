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
import org.aileme.shejiao.common.utils.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.LinkEntity;
import org.aileme.shejiao.api.service.LinkService;


/**
 * 
 *
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-26 14:05:38
 */
@RestController
@RequestMapping("/admin/link")
@Tag(name = "管理端——轮播图管理")
public class LinkController {

    @Autowired
    private LinkService linkService;
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:link:list")
    @Operation(summary = "轮播图列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = linkService.queryPage(params);

        return R.ok().put("page", page);
    }



    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:link:info")
    @Operation(summary = "轮播图详情")
    public R info(@PathVariable("id") Integer id){
		LinkEntity link = linkService.getById(id);

        return R.ok().put("link", link);
    }


    @SysLog("新增轮播图")
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:link:save")
    @Operation(summary = "新增轮播图")
    public R save(@RequestBody LinkEntity link){
        link.setCreateTime(DateUtil.nowDateTime());
		linkService.save(link);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.BANNER_LIST_KEY_MINE);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.BANNER_LIST_KEY_SQUARE);
        return R.ok();
    }



    @SysLog("修改轮播图")
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:link:update")
    @Operation(summary = "修改轮播图")
    public R update(@RequestBody LinkEntity link){
		linkService.updateById(link);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.BANNER_LIST_KEY_MINE);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.BANNER_LIST_KEY_SQUARE);
        return R.ok();
    }

    /**
     * 删除
     */
    @SysLog("删除轮播图")
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:link:delete")
    @Operation(summary = "删除轮播图")
    public R delete(@RequestBody Integer[] ids){
		linkService.removeByIds(Arrays.asList(ids));
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.BANNER_LIST_KEY_MINE);
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.BANNER_LIST_KEY_SQUARE);
        return R.ok();
    }

}
