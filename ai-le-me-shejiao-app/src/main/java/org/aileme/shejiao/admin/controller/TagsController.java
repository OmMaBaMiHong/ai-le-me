package org.aileme.shejiao.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.api.service.UserTagsService;
import org.aileme.shejiao.common.annotation.SysLog;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tags")
@Tag(name = "管理端——标签管理")
public class TagsController {

    @Autowired
    private TagsService tagsService;

    @Autowired
    private UserTagsService userTagsService;

    @GetMapping("/list")
    @Operation(summary = "标签列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = tagsService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有分类")
    public R categories() {
        List<String> categories = tagsService.getAllCategories();
        return R.ok().put("categories", categories);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "标签详情")
    public R info(@PathVariable("id") Long id) {
        TagsEntity tag = tagsService.getById(id);
        return R.ok().put("tag", tag);
    }

    @SysLog("新增标签")
    @PostMapping("/save")
    @Operation(summary = "保存标签")
    public R save(@RequestBody TagsEntity tag) {
        tagsService.saveTag(tag);
        return R.ok();
    }

    @SysLog("修改标签")
    @PostMapping("/update")
    @Operation(summary = "修改标签")
    public R update(@RequestBody TagsEntity tag) {
        tagsService.updateTag(tag);
        return R.ok();
    }

    @SysLog("删除标签")
    @PostMapping("/delete")
    @Operation(summary = "删除标签")
    public R delete(@RequestBody Integer[] ids) {
        tagsService.removeByIds(Arrays.asList(ids));
        return R.ok();
    }

    @PostMapping("/setUserTags")
    @Operation(summary = "设置用户标签")
    public R setUserTags(@RequestBody Map<String, Object> params) {
        Integer userId = Integer.valueOf(params.get("userId").toString());
        Integer tagId = Integer.valueOf(params.get("tagId").toString());
        String source = (String) params.get("source");
        Integer weight = params.get("weight") != null ? Integer.valueOf(params.get("weight").toString()) : 5;
        
        // 转换source为 sourceType
        Integer sourceType = 2; // 默认自定义
        if ("系统推荐".equals(source)) {
            sourceType = 1;
        } else if ("红娘设置".equals(source)) {
            sourceType = 2;
        } else if ("用户自选".equals(source)) {
            sourceType = 3;
        }
        
        userTagsService.setUserTags(userId, Arrays.asList(tagId), sourceType);
        return R.ok();
    }

    @GetMapping("/getUserTags/{userId}")
    @Operation(summary = "获取用户标签")
    public R getUserTags(@PathVariable("userId") Integer userId) {
        List<TagsEntity> tags = userTagsService.getTagsByUserId(userId);
        return R.ok().put("tags", tags);
    }
}
