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

import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.api.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台前端首页数据统计
 * @author linfeng
 * @date 2022/4/17 16:49
 */
@RestController
@RequestMapping("/admin/statistics")
@Tag(name = "管理端——首页数据统计")
public class StatisticController {

    @Autowired
    private AppUserService userService;


    @GetMapping("/home")
    @Operation(summary = "数据")
    public R index(){

        return R.ok().put("result",userService.indexDate());
    }


    @GetMapping("/chart")
    @Operation(summary = "统计用户")
    public R chart(){
        return R.ok().put("result",userService.chartCount());
    }

    @GetMapping("/chartPost")
    @Operation(summary = "统计发帖数")
    public R chartPost(){
        return R.ok().put("result",userService.chartPost());
    }


    @GetMapping("/chartMoney")
    @Operation(summary = "统计交易额")
    public R chartMoney(){
        return R.ok().put("result",userService.chartMoney());
    }

}
