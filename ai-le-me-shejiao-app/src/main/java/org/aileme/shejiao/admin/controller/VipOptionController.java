/**
 * -----------------------------------
 * Copyright (c) 2021-2024
 * All rights reserved, Designed By www.linfengtech.cn
 * 林风社交论坛商业版本请务必保留此注释头信息
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.admin.controller;

import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.VipOptionEntity;
import org.aileme.shejiao.api.service.VipOptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 会员充值选项
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2022-09-28 14:26:17
 */
@RestController
@RequestMapping("/admin/vipoption")
@Tag(name = "管理端——会员充值选项")
public class VipOptionController {
    @Autowired
    private VipOptionService vipOptionService;

    /**
     * 列表
     */
    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
//    "admin:vipoption:list")
@Operation(summary = "会员充值选项列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = vipOptionService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:vipoption:info")
    @Operation(summary = "会员充值选项详情")
    public R info(@PathVariable("id") Integer id){
		VipOptionEntity vipOption = vipOptionService.getById(id);

        return R.ok().put("vipOption", vipOption);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:vipoption:save")
    @Operation(summary = "会员充值选项保存")
    public R save(@RequestBody VipOptionEntity vipOption){
		vipOption.setProductType(Constant.PAY_PRODUCT_TYPE_VIP);
		vipOptionService.save(vipOption);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:vipoption:update")
    @Operation(summary = "会员充值选项修改")
    public R update(@RequestBody VipOptionEntity vipOption){
		vipOption.setProductType(Constant.PAY_PRODUCT_TYPE_VIP);
		vipOptionService.updateById(vipOption);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // "admin:vipoption:delete")
    @Operation(summary = "会员充值选项删除")
    public R delete(@RequestBody Integer[] ids){
		vipOptionService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
