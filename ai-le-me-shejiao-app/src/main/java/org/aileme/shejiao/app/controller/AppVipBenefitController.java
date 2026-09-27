package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.VipBenefitService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.VipBenefitEntity;

import java.util.List;


/**
 * 会员权益
 *
 * @author Pity
 * @email linfengtech002@163.com
 * @date 2023-12-11 22:51:19
 */
@Tag(name = "用户端——会员权益")
@RestController
@RequestMapping("app/vipbenefit")
public class AppVipBenefitController {

    @Autowired
    private VipBenefitService vipBenefitService;

    @GetMapping("/getList")
    @Operation(summary = "会员权益列表")
    public Result<List<VipBenefitEntity>> list(){
        List<VipBenefitEntity> result = vipBenefitService.getList();
        return new Result<List<VipBenefitEntity>>().ok(result);
    }


}
