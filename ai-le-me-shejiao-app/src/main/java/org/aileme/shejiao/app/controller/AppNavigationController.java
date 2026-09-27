package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.NavigationEntity;
import org.aileme.shejiao.api.service.NavigationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 导航栏模块
 *
 * @author JL.Yu
 * @email linfengtech001@163.com
 * @date 2023-02-04 22:14:59
 */
@RestController
@RequestMapping("/app/navigation")
@Tag(name = "移动端——导航栏管理")
public class AppNavigationController {


    @Autowired
    private NavigationService navigationService;


    @PostMapping("/getNav")
    @Operation(summary = "导航栏查询")
    public Result<List<NavigationEntity>> getNav(){
		List<NavigationEntity> list=navigationService.getNav();

        return new Result<List<NavigationEntity>>().ok(list);
    }

}
