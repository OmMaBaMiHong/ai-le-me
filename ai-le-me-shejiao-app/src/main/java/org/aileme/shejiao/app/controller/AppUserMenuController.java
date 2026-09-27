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
import org.aileme.shejiao.domain.entity.admin.UserMenuEntity;
import org.aileme.shejiao.api.service.UserMenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户菜单
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-07-22 09:33:30
 */
@RestController
@RequestMapping("/app/userMenu")
@Tag(name = "移动端——用户菜单")
public class AppUserMenuController {


    @Autowired
    private UserMenuService userMenuService;

    /**
     * 用户菜单列表查询
     */
    @GetMapping("/list")
    @Operation(summary = "用户菜单列表查询")
    public Result<List<UserMenuEntity>> list(){
        List<UserMenuEntity> list = userMenuService.menuList();
        return new Result<List<UserMenuEntity>>().ok(list);
    }



}
