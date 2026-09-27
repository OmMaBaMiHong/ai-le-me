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
import org.aileme.shejiao.domain.entity.admin.LinkEntity;
import org.aileme.shejiao.api.service.LinkService;
import org.aileme.shejiao.domain.param.app.LinkListForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 
 * 轮播图管理
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-26 14:05:38
 */
@RestController
@RequestMapping("/app/link")
@Tag(name = "移动端——轮播图")
public class AppLinkController {
    @Autowired
    private LinkService linkService;

    /**
     * 获取轮播图列表
     */
    @PostMapping("/list")
    @Operation(summary = "获取轮播图列表")
    public Result<List<LinkEntity>> list(@RequestBody LinkListForm request){
        List<LinkEntity> list=linkService.getPageList(request);
        return new Result<List<LinkEntity>>().ok(list);
    }


}
