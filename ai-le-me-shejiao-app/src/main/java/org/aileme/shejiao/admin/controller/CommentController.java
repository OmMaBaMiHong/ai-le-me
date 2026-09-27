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

import org.aileme.shejiao.common.annotation.NoRepeatSubmit;
import org.aileme.shejiao.common.annotation.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.CommentEntity;
import org.aileme.shejiao.api.service.CommentService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 
 * 评论管理
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 21:29:22
 */
@RestController
@RequestMapping("/admin/comment")
@Tag(name = "管理端——评论管理")
public class CommentController {
    @Autowired
    private CommentService commentService;



    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:comment:list")
    @Operation(summary = "评论列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = commentService.queryPage(params);

        return R.ok().put("page", page);
    }



    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:comment:info")
    @Operation(summary = "评论详情")
    public R info(@PathVariable("id") Long id){
		CommentEntity comment = commentService.getById(id);

        return R.ok().put("comment", comment);
    }




    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:comment:save")
    @Operation(summary = "评论保存")
    public R save(@RequestBody CommentEntity comment){
		commentService.save(comment);

        return R.ok();
    }


    @NoRepeatSubmit
    @SysLog("修改评论")
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:comment:update")
    @Operation(summary = "修改评论")
    public R update(@RequestBody CommentEntity comment){
		commentService.updateCommentById(comment);

        return R.ok();
    }



    @SysLog("删除评论")
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:comment:delete")
    @Operation(summary = "删除评论")
    @NoRepeatSubmit
    public R delete(@RequestBody Long[] ids){
        commentService.deleteByAdmin(Arrays.asList(ids));
        return R.ok();
    }

}
