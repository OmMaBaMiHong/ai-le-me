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
package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.vo.AppCashInfoResponse;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.CashOutService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.domain.param.app.AddCashOutForm;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;


/**
 * 提现模块
 *
 * @author JL.Yu
 * @date 2023-02-01 12:42:16
 */
@RestController
@RequestMapping("/app/cashOut")
@Tag(name = "移动端——提现")
public class AppCashOutController {
    @Autowired
    private CashOutService cashOutService;

    @Resource
    private MiniAppFilingFeatureService miniAppFilingFeatureService;



    @Login
    @PostMapping("/submit")
    @Operation(summary = "用户提交提现申请")
    public R save(@RequestBody AddCashOutForm param,@Parameter(hidden = true) @LoginUser AppUserEntity user){
        miniAppFilingFeatureService.requirePaymentEnabled();
        cashOutService.submit(param,user.getUid());
        return R.ok();
    }

    @Login
    @GetMapping("/getAccountBasicInfo")
    @Operation(summary = "查询账户基本信息")
    public Result<AppCashInfoResponse> getAccountBasicInfo(@Parameter(hidden = true) @LoginUser AppUserEntity user){
        miniAppFilingFeatureService.requirePaymentEnabled();
        AppCashInfoResponse vo =cashOutService.getAccountBasicInfo(user.getUid());
        return new Result<AppCashInfoResponse>().ok(vo);
    }


}
