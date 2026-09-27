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

import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.RechargeEntity;
import org.aileme.shejiao.api.service.RechargeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 
 * 充值方案管理
 * @author linfeng
 * @email linfengtech001@163.com
 * @date 2022-04-21 17:05:58
 */
@RestController
@RequestMapping("/admin/recharge")
@Tag(name = "管理端——充值方案管理")
public class RechargeController {
    @Autowired
    private RechargeService rechargeService;


    @GetMapping("/list")
    @Operation(summary = "充值方案列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = rechargeService.queryPage(params);

        return R.ok().put("page", page);
    }



    @GetMapping("/info/{id}")
    @Operation(summary = "充值方案详情")
    public R info(@PathVariable("id") Integer id){
		RechargeEntity recharge = rechargeService.getById(id);

        return R.ok().put("recharge", recharge);
    }


    @PostMapping("/save")
    @Operation(summary = "充值方案保存")
    public R save(@RequestBody RechargeEntity recharge){
		recharge.setProductType(Constant.PAY_PRODUCT_TYPE_COIN);
		rechargeService.save(recharge);

        return R.ok();
    }


    @PostMapping("/update")
    @Operation(summary = "充值方案修改")
    public R update(@RequestBody RechargeEntity recharge){
		recharge.setProductType(Constant.PAY_PRODUCT_TYPE_COIN);
		rechargeService.updateById(recharge);

        return R.ok();
    }


    @PostMapping("/delete")
    @Operation(summary = "充值方案删除")
    public R delete(@RequestBody Integer[] ids){
		rechargeService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
