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
package org.aileme.shejiao.app.controller;

import cn.hutool.core.util.ObjectUtil;
import org.aileme.shejiao.common.annotation.NoRepeatSubmit;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.PostDetailResponse;
import org.aileme.shejiao.domain.vo.PostVipInfoResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.validator.ValidatorUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.*;
import org.aileme.shejiao.api.service.PostCollectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 *
 * 帖子APP端
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 20:09:55
 */
@Tag(name = "移动端——帖子")
@RestController
@RequestMapping("/app/post")
public class AppPostController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostCollectionService postCollectionService;


    /**
     * 删除
     */
    @Login
    @PostMapping("/del")
    @Operation(summary = "用户删除帖子")
    public Result delete(@RequestBody AddPostDeleteForm request,
                         @Parameter(hidden = true) @LoginUser AppUserEntity user){

        postService.del(request.getId(),user.getUid());

        return new Result();
    }

    @GetMapping("/detail")
    @Operation(summary = "获取帖子详情")
    @Parameters({
            @Parameter(name = "id", description = "帖子id", required = true)
    })
    public Result<PostDetailResponse> detail(@RequestParam Integer id){

        PostDetailResponse response=postService.detail(id);
        return new Result<PostDetailResponse>().ok(response);
    }

    /**
     * 帖子列表分页
     * （此接口 V1.12.0 已废弃，为保证老版本过渡到新版的兼容，暂不删除，可自行删除此接口，保证数据安全）
     */
    @Deprecated
    @PostMapping("/list")
    @Operation(summary = "帖子列表分页")
    public Result<AppPageUtils> list(@RequestBody PostListForm request){

        AppPageUtils page = postService.queryPageList(request);

        return new Result<AppPageUtils>().ok(page);
    }

    /**
     * 根据话题ID查询帖子列表分页
     */
    @PostMapping("/getPostListByDiscussId")
    @Operation(summary = "根据话题ID查询帖子列表分页")
    public Result<AppPageUtils> getPostListByDiscussId(@RequestBody PostListForm request){

        AppPageUtils page = postService.getPostListByDiscussId(request);

        return new Result<AppPageUtils>().ok(page);
    }

    /**
     * 根据圈子ID查询帖子列表分页
     */
    @PostMapping("/getListByTopicId")
    @Operation(summary = "根据圈子ID查询帖子列表分页")
    public Result<AppPageUtils> getListByTopicId(@RequestBody PostListForm request){

        AppPageUtils page = postService.getListByTopicId(request);

        return new Result<AppPageUtils>().ok(page);
    }

    /**
     * 根据用户ID查询帖子列表分页
     */
    @PostMapping("/getListByUid")
    @Operation(summary = "根据用户ID查询帖子列表分页")
    public Result<AppPageUtils> getListByUid(@RequestBody PostListForm request){

        AppPageUtils page = postService.getListByUid(request);

        return new Result<AppPageUtils>().ok(page);
    }

    /**
     * 短视频列表分页
     */
    @PostMapping("/videoList")
    @Operation(summary = "短视频列表分页")
    public Result<AppPageUtils> videoList(@RequestBody VideoListForm request){

        AppPageUtils page = postService.queryShortVideoPageList(request);

        return new Result<AppPageUtils>().ok(page);
    }

    @PostMapping("/addReadCount")
    @Operation(summary = "添加浏览量")
    public R addReadCount(@RequestBody AddReadCountForm request){
        postService.addReadCount(request.getPostId());

        return R.ok();
    }

    @Login
    @PostMapping("/addComment")
    @NoRepeatSubmit
    @Operation(summary = "添加评论")
    public R addComment(@RequestBody AddCommentForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(request);
        Integer value = postService.addComment(request, user);
        if(value==1){
            return R.ok().put("check",true);
        }
        return R.ok().put("check",false);
    }

    /**
     * 帖子点赞
     */
    @Login
    @PostMapping({"/addLike", "/addCollection"})
    @NoRepeatSubmit
    @Operation(summary = "帖子点赞")
    public R addCollection(@RequestBody AddCollectionForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        postService.addCollection(request,user);

        return R.ok();
    }

    /**
     * 帖子取消点赞
     */
    @Login
    @PostMapping({"/cancelLike", "/cancelCollection"})
    @NoRepeatSubmit
    @Operation(summary = "帖子取消点赞")
    public R cancelCollection(@RequestBody AddCollectionForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        postCollectionService.cancelCollection(request,user);
        return R.ok();
    }


    @Login
    @GetMapping("/joinTopicPost")
    @Operation(summary = "用户加入圈子的动态列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<AppPageUtils> joinTopicPost(@RequestParam Integer page, @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =postService.joinTopicPost(page,user);
        return new Result<AppPageUtils>().ok(pages);
    }

    @GetMapping("/lastPost")
    @Operation(summary = "最新动态列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "classId", description = "圈子分类id")
    })
    public Result<AppPageUtils> lastPost(@RequestParam Integer page,
                                         @RequestParam(required = false) Integer classId){

        AppPageUtils pages =postService.lastPost(page, classId);
        return new Result<AppPageUtils>().ok(pages);
    }



    @Login
    @GetMapping("/followUserPost")
    @Operation(summary = "获取关注用户帖子")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<Object> followUserPost(@RequestParam Integer page, @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =postService.followUserPost(page,user);
        if(ObjectUtil.isNull(pages)){
            return new Result<>().error("您没有关注的用户");
        }
        return new Result<>().ok(pages);
    }


    @Login
    @PostMapping("/addPost")
    @Operation(summary = "发帖子")
    public R addPost(@RequestBody AddPostForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(request);
        Integer id=postService.addPost(request,user);
        if(id==0){
            return R.error("发布帖子失败");
        }
        return R.ok().put("result",id);
    }

    @Login
    @GetMapping("/qrCode")
    @Operation(summary = "分享二维码")
    @Parameters({
            @Parameter(name = "postId", description = "帖子id", required = true),
            @Parameter(name = "origin", description = "来源", required = true),
            @Parameter(name = "url", description = "url分享路径", required = true)
    })
    public R qrCode(@RequestParam("postId") Integer postId,
                    @RequestParam("origin") String origin,
                    @RequestParam("url") String url,
                    @Parameter(hidden = true) @LoginUser AppUserEntity user) throws Exception {


        String resultUrl = postService.getSharePic(postId,origin,url,user);
        return R.ok().put("result",resultUrl);
    }

    @Login
    @GetMapping({"/myLikePost", "/myCollectPost"})
    @Operation(summary = "我点赞的帖子")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<AppPageUtils> myCollectPost(@RequestParam("page") Integer page, @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =postService.myCollectPost(page,user);
        return new Result<AppPageUtils>().ok(pages);
    }

    @Login
    @GetMapping("/myPost")
    @Operation(summary = "我的帖子")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<AppPageUtils> myPost(@RequestParam("page") Integer page, @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =postService.myPost(page,user);
        return new Result<AppPageUtils>().ok(pages);
    }

    @GetMapping("/getPostListByType")
    @Operation(summary = "帖子分类列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "type", description = "类型", required = true)
    })
    public Result<AppPageUtils> getPostListByType(@RequestParam("page") Integer page,
                                                  @RequestParam("type") Integer type){

        AppPageUtils pages =postService.getPostListByType(page,type);
        return new Result<AppPageUtils>().ok(pages);
    }


    @Login
    @GetMapping("/search")
    @Operation(summary = "搜索帖子")
    @NoRepeatSubmit(lockTime = 2000)
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "keyword", description = "搜索关键词")
    })
    public Result<AppPageUtils> search(@RequestParam("page") Integer page,
                                       @RequestParam("keyword") String keyword){

        AppPageUtils pages =postService.search(page,keyword);
        return new Result<AppPageUtils>().ok(pages);
    }

    @Login
    @PostMapping("/setAdmin")
    @Operation(summary = "设置圈子管理员")
    public Result setAdmin(@RequestBody SetAdminForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        Boolean setAdmin=postService.setAdmin(request,user);
        if(!setAdmin){
            return new Result().error("设置管理员失败");
        }
        return new Result();
    }

    @Login
    @PostMapping("/setPostTop")
    @Operation(summary = "设置圈内置顶")
    public Result setPostTop(@RequestBody SetPostTopForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        Boolean setAdmin=postService.setPostTop(request,user);
        if(!setAdmin){
            return new Result().error("设置圈内置顶失败");
        }
        return new Result();
    }

    @Login
    @PostMapping("/topPostDel")
    @Operation(summary = "解除圈内置顶")
    public Result topPostDel(@RequestBody SetPostTopForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        Boolean cancelAdmin=postService.topPostDel(request,user);
        if(!cancelAdmin){
            return new Result().error("解除圈内置顶失败");
        }
        return new Result();
    }

    @Login
    @PostMapping("/cancelAdmin")
    @Operation(summary = "解除圈子管理员")
    public Result cancelAdmin(@RequestBody SetAdminForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        Boolean cancelAdmin=postService.cancelAdmin(request,user);
        if(!cancelAdmin){
            return new Result().error("解除圈子管理员失败");
        }
        return new Result();
    }

    @Login
    @PostMapping("/getVipPostInfo")
    @Operation(summary = "获取用户是否付费")
    public Result<PostVipInfoResponse> getVipPostInfo(@RequestBody VipPostInfoForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        request.setUid(user.getUid());
        PostVipInfoResponse response=postService.getVipPostInfo(request);
        return new Result<PostVipInfoResponse>().ok(response);
    }


    @Login
    @PostMapping("/voteAdd")
    @Operation(summary = "发起投票")
    public Result voteAdd(@RequestBody AddVoteForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(request);
        Integer id=postService.voteAdd(request,user);
        if(id==0){
            return new Result().error("发起投票失败");
        }
        return new Result<>().ok(id);
    }

    @Login
    @PostMapping("/vote/userVote")
    @Operation(summary = "用户投票")
    public Result userVote(@RequestBody UserVoteForm request,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        postService.userVote(request,user);

        return new Result();
    }

    @GetMapping("/hotPost")
    @Operation(summary = "帖子热榜")
    public R hotPost(){
        List<PostEntity> hotList= postService.getTopPost();
        List<PostEntity> hotPost= postService.getHotPost();
        return R.ok().put("hotList",hotList).put("hotPost",hotPost);
    }

    @Login
    @PostMapping("/addArticle")
    @Operation(summary = "发文章")
    public R addArticle(@RequestBody AddArticleForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(request);
        Integer id=postService.addArticle(request,user.getUid());
        if(id==0){
            return R.error("发布文章失败");
        }
        return R.ok().put("result",id);
    }

}
