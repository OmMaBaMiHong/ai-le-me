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

import org.apache.commons.lang3.StringUtils;
import org.aileme.shejiao.domain.vo.SignResponse;
import org.aileme.shejiao.domain.vo.SignUserResponse;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.BillEntity;
import org.aileme.shejiao.domain.entity.admin.SignConfigEntity;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.UserSignService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 积分签到
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-05-07 15:01:03
 */
@RestController
@RequestMapping("/app/sign")
@Tag(name = "移动端——积分签到")
public class AppUserSignController {

    @Autowired
    private UserSignService userSignService;

    @Autowired
    private BillService billService;


    @Login
    @PostMapping("/userInfo")
    @Operation(summary = "获取签到用户信息")
    public R userInfo(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        SignUserResponse vo=userSignService.getUserInfo(user);
        return R.ok().put("data",vo);
    }

    @Login
    @PostMapping("/sign")
    @Operation(summary = "用户签到")
    public R sign(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        int num = userSignService.sign(user);
        return R.ok().put("integral",num);
    }


    @Login
    @GetMapping("/signList")
    @Operation(summary = "用户签到信息列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "limit", description = "每页数量", required = true)
    })
    public R signList(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                      @RequestParam("page") Integer page,
                      @RequestParam("limit") Integer limit){
        List<SignResponse> list=userSignService.getSignList(user.getUid(),page,limit);
        return R.ok().put("data",list);
    }

    @GetMapping("/config")
    @Operation(summary = "签到配置")
    public R signConfig(){
        List<SignConfigEntity> list=userSignService.getConfigList();
        return R.ok().put("data",list);
    }

    @Login
    @GetMapping("/integralList")
    @Operation(summary = "用户签到积分列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "limit", description = "每页数量", required = true),
            @Parameter(name = "type", description = "类型", required = true)
    })
    public R integralList(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                          @RequestParam("page") Integer page,
                          @RequestParam("limit") Integer limit,
                          @RequestParam("type") Integer type){
        List<BillEntity> list= billService.getIntegralList(user.getUid(),page,limit,type);
        return R.ok().put("data",list);
    }


    @Login
    @PostMapping("/consume/integral")
    @Operation(summary = "消耗积分")
    public R consumeIntegral(@Parameter(hidden = true) @LoginUser AppUserEntity user,
                             @RequestParam("type") Integer type
                             ){
        String msg= userSignService.consumeIntegral(user,type);
        if(StringUtils.isNotBlank(msg)){
            return R.error(msg);
        }
        return R.ok();
    }

}
