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
import org.aileme.shejiao.domain.entity.admin.VipBenefitEntity;
import org.aileme.shejiao.domain.vo.OrderPay;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.VipOptionEntity;
import org.aileme.shejiao.api.service.VipOptionService;
import org.aileme.shejiao.api.service.VipBenefitService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.domain.param.app.VipPayForm;
import org.aileme.shejiao.domain.param.app.VipRechargeForm;
import org.aileme.shejiao.api.service.SysConfigService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 会员充值选项
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-09-28 14:26:17
 */
@RestController
@RequestMapping("/app/vip")
@Tag(name = "移动端——会员充值选项")
public class AppVipOptionController {

    @Autowired
    private VipOptionService vipOptionService;

    @Autowired
    private VipBenefitService vipBenefitService;

    @Autowired
    private SysConfigService configService;


    @GetMapping("/vipList")
    @Operation(summary = "会员充值选项列表")
    public Result<List<VipOptionEntity>> vipList(){
        List<VipOptionEntity> result = vipOptionService.lambdaQuery()
                .eq(VipOptionEntity::getProductType, Constant.PAY_PRODUCT_TYPE_VIP)
                .eq(VipOptionEntity::getStatus, 0)
                .orderByAsc(VipOptionEntity::getSort)
                .list();
        return new Result<List<VipOptionEntity>>().ok(result);
    }


    @GetMapping("/vipConfig")
    @Operation(summary = "会员配置查询")
    public R vipConfig(){
        return R.ok()
                .put("integralNum", configService.getValue(Constant.VIP_INTEGRAL))
                .put("renameCount",configService.getValue(Constant.VIP_RENAME))
                .put("topicNum",configService.getValue(Constant.VIP_TOPIC_NUMBER))
                //隐私设置
                //广告屏蔽
                //创建付费帖
                //回看
                .put("adBlock",configService.getValue(Constant.VIP_AD_BLOCK))
                .put("aiChat",configService.getValue(Constant.VIP_AI_CHAT))
                .put("paidPost",configService.getValue(Constant.VIP_PAID_POST))
                ;
    }

    @GetMapping("/benefitList")
    @Operation(summary = "VIP会员权益列表")
    public Result<List<VipBenefitEntity>> benefitList(){
        List<VipBenefitEntity> result = vipBenefitService.lambdaQuery()
                .eq(VipBenefitEntity::getStatus, 0)
                .orderByDesc(VipBenefitEntity::getSort)
                .orderByAsc(VipBenefitEntity::getId)
                .list();
        return new Result<List<VipBenefitEntity>>().ok(result);
    }


    @Login
    @PostMapping("/rechargeVip")
    @Operation(summary = "会员预充值")
    public R rechargeVip(@Parameter(hidden = true) @LoginUser AppUserEntity user, @RequestBody VipRechargeForm param){

        String orderId = vipOptionService.rechargeVip(user,param);
        return R.ok().put("result",orderId);
    }

    @Login
    @PostMapping("/pay")
    @Operation(summary = "会员充值微信支付")
    public R pay(@Parameter(hidden = true) @LoginUser AppUserEntity user, @RequestBody VipPayForm param, HttpServletRequest request) throws Exception{

        OrderPay orderPay=vipOptionService.vipPay(user,param,request);
        return R.ok().put("data",orderPay);
    }

}
