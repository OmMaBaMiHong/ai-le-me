/**
 * -----------------------------------
 *  Copyright (c) 2021-2023

 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.controller;

import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.CategoryEntity;
import org.aileme.shejiao.api.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 圈子分类
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-21 14:32:52
 */
@RestController
@RequestMapping("app")
@Tag(name = "移动端——圈子分类")
public class AppCategoryController {


    @Autowired
    private CategoryService categoryService;

    /**
     * 分类列表
     */
    @GetMapping("/topic/classList")
    @Operation(summary = "分类列表")
    public Result<List<CategoryEntity>> list(){
        List<CategoryEntity> result = categoryService.getList();
        return new Result<List<CategoryEntity>>().ok(result);
    }


}
