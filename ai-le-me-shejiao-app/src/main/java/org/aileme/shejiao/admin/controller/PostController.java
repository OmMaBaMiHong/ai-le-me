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

import org.aileme.shejiao.common.validator.ValidatorUtils;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.param.app.AddPostByAdminForm;
import org.aileme.shejiao.domain.param.app.DeletePostForm;
import org.aileme.shejiao.domain.param.app.DownPostForm;


/**
 * 
 * 帖子管理
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 20:49:55
 */
@RestController
@RequestMapping("/admin/post")
@Tag(name = "管理端——帖子管理")
public class PostController {
    @Autowired
    private PostService postService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:list")
    @Operation(summary = "帖子列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = postService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:info")
    @Operation(summary = "帖子详情")
    public R info(@PathVariable("id") Integer id){
		PostEntity post = postService.getById(id);

        return R.ok().put("post", post);
    }



    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:save")
    @Operation(summary = "帖子保存")
    @NoRepeatSubmit
    public R save(@RequestBody PostEntity post){
		postService.save(post);

        return R.ok();
    }



    @SysLog("修改帖子")
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:update")
    @Operation(summary = "修改帖子")
    @NoRepeatSubmit
    public R update(@RequestBody PostEntity post){
		postService.updateById(post);

        return R.ok();
    }



    @SysLog("批量删除帖子")
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:delete")
    @Operation(summary = "批量删除帖子")
    @NoRepeatSubmit
    public R delete(@RequestBody Integer[] ids){
        postService.deleteByAdmin(Arrays.asList(ids));
        return R.ok();
    }

    @SysLog("删除帖子")
    @PostMapping("/deleteById")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:delete")
    @Operation(summary = "删除帖子")
    @NoRepeatSubmit
    public R deleteById(@RequestBody DeletePostForm param){
        postService.deletePostIdByAdmin(param);
        return R.ok();
    }


    @SysLog("下架帖子")
    @PostMapping("/down")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:delete")
    @Operation(summary = "下架帖子")
    @NoRepeatSubmit
    public R down(@RequestBody DownPostForm param){
        postService.downByAdmin(param);
        return R.ok();
    }

    @SysLog("上架帖子")
    @PostMapping("/up")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:delete")
    @Operation(summary = "上架帖子")
    @NoRepeatSubmit
    public R up(@RequestBody Integer[] ids){
        postService.upByAdmin(Arrays.asList(ids));
        return R.ok();
    }


    @PostMapping("/postSubmit")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:post:save")
    @Operation(summary = "发帖子")
    @NoRepeatSubmit
    public R postSubmit(@RequestBody AddPostByAdminForm request){
        ValidatorUtils.validateEntity(request);
        postService.postAddByAdmin(request);

        return R.ok();
    }
}
