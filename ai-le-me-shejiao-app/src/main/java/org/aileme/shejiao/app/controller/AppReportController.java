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
import org.aileme.shejiao.domain.vo.AppReportListResponse;
import org.aileme.shejiao.common.utils.AppPageUtils;
import org.aileme.shejiao.common.validator.ValidatorUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.ReportService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.ReportAddForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 用户举报
 *
 * @author linfeng
 * @email 3582996245@qq.com
 * @date 2022-09-01 12:55:12
 */
@RestController
@RequestMapping("/app/report")
@Tag(name = "移动端——用户举报")
public class AppReportController {

    @Autowired
    private ReportService reportService;


    @Login
    @PostMapping("/addReport")
    @Operation(summary = "用户提交举报")
    public Result save(@RequestBody ReportAddForm request, @Parameter(hidden = true) @LoginUser AppUserEntity user){
        ValidatorUtils.validateEntity(request);
		reportService.addReport(request,user);

        return new Result();
    }

    @Login
    @GetMapping("/list")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "status", description = "状态0待审核 1已处理 2已驳回", required = true)
    })
    @Operation(summary = "用户举报分页")
    public Result<AppPageUtils> list(@RequestParam("page") Integer page,@RequestParam("status") Integer status,@Parameter(hidden = true) @LoginUser AppUserEntity user){

        AppPageUtils pages =reportService.listByUser(page,status,user);
        return new Result<AppPageUtils>().ok(pages);
    }


    @Login
    @GetMapping("/detail")
    @Operation(summary = "用户举报详情")
    @Parameters({
            @Parameter(name = "id", description = "举报单号id", required = true)
    })
    public Result<AppReportListResponse> info(@RequestParam("id") Integer id){

        AppReportListResponse report = reportService.detail(id);
        return new Result<AppReportListResponse>().ok(report);
    }

}
