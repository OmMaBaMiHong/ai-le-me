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

import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.api.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * 机器人自动发帖
 * @author linfeng
 * @date 2022/7/21 11:25
 */
@RestController
@Tag(name = "管理端——机器人自动注册发帖")
public class RobotController {


    @Autowired
    private PostService postService;



    @GetMapping("/list")
    @Operation(summary = "执行自动注册发帖")
    public R list(){
        postService.getRobotPostContent();
        return R.ok();
    }

}
