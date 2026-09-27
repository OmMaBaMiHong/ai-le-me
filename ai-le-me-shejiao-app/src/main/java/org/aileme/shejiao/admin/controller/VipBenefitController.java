package org.aileme.shejiao.admin.controller;

import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.VipBenefitEntity;
import org.aileme.shejiao.api.service.VipBenefitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;



/**
 * VIP会员权益设置
 *
 * @author Wade
 * @date 2026-02-10
 */
@RestController
@RequestMapping("/admin/vipbenefit")
@Tag(name = "管理端——VIP会员权益设置")
public class VipBenefitController {
    @Autowired
    private VipBenefitService vipBenefitService;

    /**
     * 列表
     */
    @GetMapping("/list")
    @Operation(summary = "VIP会员权益列表分页")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = vipBenefitService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    @Operation(summary = "VIP会员权益详情")
    public R info(@PathVariable("id") Integer id){
		VipBenefitEntity vipBenefit = vipBenefitService.getById(id);

        return R.ok().put("vipBenefit", vipBenefit);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    @Operation(summary = "VIP会员权益保存")
    public R save(@RequestBody VipBenefitEntity vipBenefit){
        vipBenefit.setCreateTime(new Date());
        vipBenefit.setUpdateTime(new Date());
		vipBenefitService.save(vipBenefit);

        return R.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @Operation(summary = "VIP会员权益修改")
    public R update(@RequestBody VipBenefitEntity vipBenefit){
        vipBenefit.setUpdateTime(new Date());
		vipBenefitService.updateById(vipBenefit);

        return R.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @Operation(summary = "VIP会员权益删除")
    public R delete(@RequestBody Integer[] ids){
		vipBenefitService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
