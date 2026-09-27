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

import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.ConfigConstant;
import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.api.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
/* // import org.apache.shiro.authz.annotation.RequiresPermissions; // Temporarily removed due to Spring Boot 3.x compatibility */ // Temporarily removed due to Spring Boot 3.x compatibility
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.aileme.shejiao.domain.entity.admin.CategoryEntity;
import org.aileme.shejiao.api.service.CategoryService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;



/**
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-21 14:32:52
 */
@RestController
@RequestMapping("/admin/category")
@Tag(name = "管理端——圈子分类管理")
public class CategoryController {


    @Autowired
    private CategoryService categoryService;
    @Autowired
    private TopicService topicService;


    @GetMapping("/list")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:category:list")
    @Operation(summary = "分类列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryService.queryPage(params);

        return R.ok().put("page", page);
    }

    @GetMapping("/getList")
    @Operation(summary = "分类列表-全部-不分页")
    public R getList(){
        List<CategoryEntity> list = categoryService.getList();

        return R.ok().put("result", list);
    }



    @GetMapping("/info/{cateId}")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:category:info")
    @Operation(summary = "分类详情")
    public R info(@PathVariable("cateId") Integer cateId){
		CategoryEntity category = categoryService.getById(cateId);

        return R.ok().put("category", category);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:category:save")
    @Operation(summary = "分类保存")
    public R save(@RequestBody CategoryEntity category){
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.TOPIC_CATEGORY_KEY);
		categoryService.saveCategory(category);

        return R.ok();
    }


    @SysLog("修改圈子分类")
    @PostMapping("/update")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:category:update")
    @Operation(summary = "修改分类")
    public R update(@RequestBody CategoryEntity category){
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.TOPIC_CATEGORY_KEY);
		categoryService.updateById(category);

        return R.ok();
    }


    @SysLog("删除圈子分类")
    @PostMapping("/delete")
    /* @RequiresPermissions */ // Temporarily removed due to Spring Boot 3.x compatibility
    // //    "admin:category:delete")
    @Operation(summary = "删除分类")
    public R delete(@RequestBody Integer[] cateIds){
        List<Integer> list=Arrays.asList(cateIds);
        list.forEach(id->{
            long count = topicService.lambdaQuery().eq(TopicEntity::getCateId, id).count();
            if(count>0){
                throw new LinfengException("分类下存在圈子无法删除");
            }
        });
        org.aileme.common.redis.utils.RedisUtils.deleteObject(ConfigConstant.TOPIC_CATEGORY_KEY);
		categoryService.removeByIds(Arrays.asList(cateIds));

        return R.ok();
    }

}
