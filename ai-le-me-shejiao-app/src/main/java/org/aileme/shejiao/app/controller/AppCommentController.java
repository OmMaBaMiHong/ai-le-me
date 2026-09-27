/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.annotation.NoRepeatSubmit;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.CommentService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.AddThumbsForm;
import org.aileme.shejiao.domain.param.app.DelCommentForm;
import org.aileme.shejiao.api.service.CommentThumbsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 *
 * APP评论接口
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 21:29:22
 */
@RestController
@RequestMapping("/app/comment")
@Tag(name = "移动端——用户评论")
public class AppCommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private CommentThumbsService commentThumbsService;

    /**
     * 评论列表
     */
    @GetMapping("/list")
    @Operation(summary = "评论列表")
    @Parameters({
            @Parameter(name = "postId", description = "帖子id", required = true),
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<Object> list(@RequestParam("postId")Integer postId, @RequestParam("page")Integer page){
         if(page==1){
            return new Result<>().ok(commentService.getCommentListInCache(postId));
        }
        AppPageUtils pages =commentService.queryCommentPage(postId,page);
        return new Result<>().ok(pages);
    }



    /**
     * 单条评论删除
     */
    @Login
    @PostMapping("/del")
    @Operation(summary = "单条评论删除")
    public Result del(@RequestBody DelCommentForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
		commentService.del(request,user);

        return new Result();
    }


    /**
     * 评论区的点赞
     */
    @Login
    @PostMapping("/thumbs")
    @NoRepeatSubmit
    @Operation(summary = "评论区的点赞")
    public R thumbs(@RequestBody AddThumbsForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){

        commentThumbsService.addThumbs(request,user);
        return R.ok();
    }

    /**
     * 取消评论区的点赞
     */
    @Login
    @PostMapping("/cancelThumbs")
    @NoRepeatSubmit
    @Operation(summary = "取消评论区的点赞")
    public R cancelThumbs(@RequestBody AddThumbsForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){

        commentThumbsService.cancelThumbs(request,user);
        return R.ok();
    }

}
