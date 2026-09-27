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

import org.aileme.shejiao.common.utils.*;
import org.aileme.shejiao.domain.vo.TopicDetailResponse;
import org.aileme.shejiao.domain.vo.TopicListResponse;
import org.aileme.shejiao.common.validator.ValidatorUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.CategoryEntity;
import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.CategoryService;
import org.aileme.shejiao.api.service.TopicService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.TopicAddForm;
import org.aileme.shejiao.domain.param.app.TopicUpdateForm;
import org.aileme.shejiao.domain.param.app.TopicUserForm;
import org.aileme.shejiao.domain.param.app.UserJoinTopicForm;
import org.aileme.shejiao.api.service.SysConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * App端 圈子接口
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-21 17:01:12
 */
@RestController
@RequestMapping("/app/topic")
@Tag(name = "移动端——圈子接口")
public class AppTopicController {


    @Autowired
    private TopicService topicService;
    @Autowired
    private AppUserService appUserService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SysConfigService configService;
    @GetMapping("/list")
    @Operation(summary = "圈子列表分页")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
            @Parameter(name = Constant.ORDER_FIELD, description = "排序字段"),
            @Parameter(name = Constant.ORDER, description = "排序方式，可选值(asc、desc)"),
            @Parameter(name = Constant.CLASSID, description = "分类id", required = true)
    })
    public R list(@Parameter(hidden = true) @RequestParam Map<String, Object> params){

        AppPageUtils page = topicService.queryByPageList(params);
        return R.ok().put("result", page);
    }


    /**
     * 创建圈子
     */
    @Login
    @PostMapping("/topicAdd")
    @Operation(summary = "创建圈子")
    public Result save(@RequestBody TopicAddForm topic,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(topic);
        Integer id = topicService.topicAdd(topic, user);
        if(id!=0){
            return new Result<>().ok(id);
        }
        return new Result().error("创建圈子失败");
    }

    /**
     * 编辑圈子
     */
    @Login
    @PostMapping("/topicEdit")
    @Operation(summary = "编辑圈子")
    public Result topicEdit(@RequestBody TopicUpdateForm topic,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(topic);
        Boolean update = topicService.topicEdit(topic, user);
        if(update){
            org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.H5_TOPIC_POSTER_KEY + topic.getId());
            org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.WX_TOPIC_POSTER_KEY + topic.getId());
            return new Result();
        }
        return new Result().error();
    }


    /**
     * 圈子详情
     */
    @Operation(summary = "圈子详情")
    @GetMapping("/detail")
    @Parameters({
            @Parameter(name = "id", description = "圈子id", required = true)
    })
    public Result<TopicDetailResponse> detail(@RequestParam("id")Integer id){

        TopicDetailResponse topic=topicService.detail(id);
        return new Result<TopicDetailResponse>().ok(topic);
    }

    @Operation(summary = "圈子用户列表分页")
    @PostMapping("/user")
    @Login
    public Result<AppPageUtils> user(@RequestBody TopicUserForm form,@Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils page =appUserService.findTopicUserPage(form,user);

        return new Result<AppPageUtils>().ok(page);
    }

    @Login
    @GetMapping("/joinTopic")
    @Operation(summary = "加入圈子")
    @Parameters({
            @Parameter(name = "id", description = "圈子id", required = true)
    })
    public Result joinTopic(@RequestParam("id")Integer id,
                       @Parameter(hidden = true) @LoginUser AppUserEntity user){

        topicService.joinTopic(id,user);
        return new Result();
    }

    @Operation(summary = "退出圈子")
    @GetMapping("/userTopicDel")
    @Login
    @Parameters({
            @Parameter(name = "id", description = "圈子id", required = true)
    })
    public Result userTopicDel(@RequestParam("id")Integer id,
                               @Parameter(hidden = true) @LoginUser AppUserEntity user){

        topicService.userTopicDel(id,user);
        return new Result();
    }

    @Operation(summary = "圈主解散圈子")
    @GetMapping("/topicDel")
    @Login
    @Parameters({
            @Parameter(name = "id", description = "圈子id", required = true)
    })
    public Result topicDel(@RequestParam("id")Integer id,
                           @Parameter(hidden = true) @LoginUser AppUserEntity user){

        topicService.topicDel(id,user);
        return new Result();
    }


    @Operation(summary = "查找用户加入的圈子")
    @PostMapping("/userJoinTopic")
    @Login
    public Result<AppPageUtils> userJoinTopic(@RequestBody UserJoinTopicForm request,
                                              @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils page=topicService.userJoinTopic(request,user);
        return new Result<AppPageUtils>().ok(page);
    }

    @Operation(summary = "热门圈子")
    @GetMapping("/hot")
    public Result<List<TopicListResponse>> hot(){

        List<TopicListResponse> list=topicService.hotTopic();
        return new Result<List<TopicListResponse>>().ok(list);
    }

    @Operation(summary = "公告内容")
    @GetMapping("/notice")
    public Result<List<String>> notice(){
        String value = configService.getValue(Constant.NOTICE_CONTENT);
        List<String> list=new ArrayList<>();
        list.add(value);
        return new Result<List<String>>().ok(list);
    }


    @Operation(summary = "圈子列表分页展示并带三张图")
    @GetMapping("/classTopicAreImg")
    @Parameters({
            @Parameter(name = "classId", description = "分类id", required = true),
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result<AppPageUtils> classTopicAreImg(@RequestParam("classId")Integer classId,
                              @RequestParam("page")Integer page){

        AppPageUtils pages=topicService.classTopicAreImg(classId,page);
        return new Result<AppPageUtils>().ok(pages);
    }


    @Operation(summary = "用户自己的圈子列表")
    @Login
    @GetMapping("/myCreateTopic")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true)
    })
    public Result myCreateTopic(@RequestParam("page")Integer page,
                           @Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages=topicService.myCreateTopic(page,user);
        return new Result<AppPageUtils>().ok(pages);
    }

    @Operation(summary = "查找分类")
    @PostMapping("/classList")
    @Login
    public Result<List<CategoryEntity>> classList(){

        List<CategoryEntity> list = categoryService.list();
        return new Result<List<CategoryEntity>>().ok(list);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索圈子")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "keyword", description = "搜索关键词")
    })
    public Result<AppPageUtils> search(@RequestParam("page") Integer page,
                                       @RequestParam("keyword") String keyword){

        AppPageUtils pages =topicService.search(page,keyword);
        return new Result<AppPageUtils>().ok(pages);
    }


    @Login
    @GetMapping("/detection")
    @Operation(summary = "检测用户是否可以创建圈子")
    public Result detection(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        boolean result=topicService.detection(user.getUid());
        return new Result<>().ok(result);
    }

    @Login
    @GetMapping("/qrCode")
    @Operation(summary = "分享二维码")
    @Parameters({
            @Parameter(name = "topicId", description = "圈子id", required = true),
            @Parameter(name = "origin", description = "来源", required = true),
            @Parameter(name = "url", description = "url分享路径", required = true)
    })
    public R qrCode(@RequestParam("topicId") Integer topicId,
                    @RequestParam("origin") String origin,
                    @RequestParam("url") String url,
                    @Parameter(hidden = true) @LoginUser AppUserEntity user) throws Exception {


        String resultUrl = topicService.getQrCode(topicId,origin,url,user);
        return R.ok().put("result",resultUrl);
    }
}
