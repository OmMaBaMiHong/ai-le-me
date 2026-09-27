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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.aileme.common.satoken.utils.LoginHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.UserRechargeEntity;
import org.aileme.shejiao.domain.entity.admin.UserRechargeRefundEntity;
import org.aileme.shejiao.api.service.UserRechargeService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.param.app.AdminRechargeRefundForm;



/**
 * 用户充值
 *
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-19 19:27:33
 */
@RestController
@RequestMapping("/admin/userrecharge")
@Tag(name = "管理端——用户充值管理")
public class UserRechargeController {

    @Autowired
    private UserRechargeService userRechargeService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:userrecharge:list")
    @Operation(summary = "用户充值列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = userRechargeService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:userrecharge:info")
    @Operation(summary = "用户充值详情")
    public R info(@PathVariable("id") Integer id){
		UserRechargeEntity userRecharge = userRechargeService.getById(id);

        return R.ok().put("userRecharge", userRecharge);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:userrecharge:save")
    @Operation(summary = "用户充值保存")
    public R save(@RequestBody UserRechargeEntity userRecharge){
		userRechargeService.save(userRecharge);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:userrecharge:update")
    @Operation(summary = "用户充值修改")
    public R update(@RequestBody UserRechargeEntity userRecharge){
		userRechargeService.updateById(userRecharge);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:userrecharge:delete")
    @Operation(summary = "用户充值删除")
    public R delete(@RequestBody Integer[] ids){
		userRechargeService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("/refund")
    @Operation(summary = "订单退款")
    public R refund(@RequestBody AdminRechargeRefundForm form) {
        userRechargeService.refundOrder(form.getOrderId(), form.getRefundAmount(), form.getReason(), LoginHelper.getUsername());
        return R.ok();
    }

    @GetMapping("/refundList")
    @Operation(summary = "订单退款记录")
    public R refundList(@RequestParam(required = false) String orderId) {
        List<UserRechargeRefundEntity> list = userRechargeService.refundList(orderId);
        return R.ok().put("list", list);
    }

}
