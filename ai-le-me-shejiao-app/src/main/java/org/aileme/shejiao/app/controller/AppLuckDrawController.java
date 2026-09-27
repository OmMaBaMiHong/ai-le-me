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

import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.LuckdrawService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;


/**
 * 抽奖模块
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-08-14 14:28:48
 */
@RestController
@RequestMapping("/app/luckDraw")
@Tag(name = "移动端——抽奖模块")
public class AppLuckDrawController {

    @Autowired
    private LuckdrawService luckdrawService;

    @Login
    @GetMapping("/getPrize")
    @Operation(summary = "获取奖品信息")
    public Result<Map<String,Object>> getPrize(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        Map<String,Object> map=luckdrawService.getPrize(user);
        return new Result<Map<String,Object>>().ok(map);
    }

    @Login
    @GetMapping("/start")
    @Operation(summary = "用户抽奖")
    public Result<Map<String,Object>> start(@Parameter(hidden = true) @LoginUser AppUserEntity user){

        Map<String,Object> map=luckdrawService.startLuckDraw(user);
        return new Result<Map<String,Object>>().ok(map);
    }

    @Login
    @GetMapping("/record/{page}")
    @Operation(summary = "抽奖记录")
    public Result<AppPageUtils> record(@Parameter(hidden = true) @LoginUser AppUserEntity user,@PathVariable("page") Integer page){

        AppPageUtils pages=luckdrawService.record(user,page);
        return new Result<AppPageUtils>().ok(pages);
    }




}
