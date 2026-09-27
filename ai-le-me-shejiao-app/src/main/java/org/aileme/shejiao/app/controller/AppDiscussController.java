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

import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.DiscussDetailResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.DiscussEntity;
import org.aileme.shejiao.api.service.DiscussService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.DiscussAddForm;
import org.aileme.shejiao.domain.param.app.DiscussDeleteForm;
import org.aileme.shejiao.domain.param.app.DiscussListForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/app/discuss")
@Tag(name = "移动端——话题")
public class AppDiscussController {

    @Autowired
    private DiscussService discussService;

    /**
     * 话题列表
     */
    @PostMapping("/list")
    @Operation(summary = "话题列表")
    public Result<AppPageUtils> list(@RequestBody DiscussListForm request){
        AppPageUtils page = discussService.getDiscussList(request);

        return new Result<AppPageUtils>().ok(page);
    }

    /**
     * 我创建的话题
     */
    @Login
    @PostMapping("/myDis")
    @Operation(summary = "我创建的话题")
    public Result<AppPageUtils> myDiscuss(@RequestBody DiscussListForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        AppPageUtils page = discussService.myDiscuss(request,user);

        return new Result<AppPageUtils>().ok(page);
    }

    @Login
    @PostMapping("/del")
    @Operation(summary = "圈主删除话题")
    public Result deleteDiscuss(@RequestBody DiscussDeleteForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        discussService.deleteDiscuss(request,user);
        return new Result();
    }

    @Login
    @PostMapping("/addDis")
    @Operation(summary = "圈主创建话题")
    public Result addDiscuss(@RequestBody DiscussAddForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        Boolean success=discussService.addDiscuss(request,user);
        if(success){
            return new Result().ok("话题创建完成");
        }
        return new Result().error("话题创建失败");
    }

    @GetMapping("/detail")
    @Operation(summary = "话题详情页")
    @Parameters({
            @Parameter(name = "id", description = "话题id", required = true)
    })
    public Result<DiscussDetailResponse> detail(@RequestParam Integer id){
        DiscussDetailResponse response=discussService.detail(id);

        return new Result<DiscussDetailResponse>().ok(response);
    }


    @GetMapping("/discussList")
    @Operation(summary = "热门话题")
    public Result<List<DiscussEntity>> discussList(){
        List<DiscussEntity> list=discussService.discussList();

        return new Result<List<DiscussEntity>>().ok(list);
    }



}
