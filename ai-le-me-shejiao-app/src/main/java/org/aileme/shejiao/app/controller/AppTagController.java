package org.aileme.shejiao.app.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.api.service.PostTagService;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.api.service.UserTagsService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.entity.admin.PostTagEntity;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;
import org.aileme.shejiao.domain.entity.admin.UserTagsEntity;
import org.aileme.shejiao.domain.vo.PostDetailResponse;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * App端话题功能
 */
@RestController
@RequestMapping("/app/tag")
@Tag(name = "移动端——话题功能")
public class AppTagController {
    private static final int FOLLOW_SOURCE_TYPE = 4;

    @Autowired
    private TagsService tagsService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private PostTagService postTagService;

    @Autowired
    private UserTagsService userTagsService;

    @GetMapping("/hotList")
    @Operation(summary = "热门话题列表")
    public Result<List<TagsEntity>> hotList() {
        List<TagsEntity> list = tagsService.lambdaQuery()
                .eq(TagsEntity::getStatus, 1)
                .orderByDesc(TagsEntity::getUsageCount)
                .last("LIMIT 20")
                .list();
        return new Result<List<TagsEntity>>().ok(list);
    }

    @GetMapping("/categoryList")
    @Operation(summary = "分类话题列表")
    public Result<List<TagsEntity>> categoryList(@RequestParam(required = false) String category) {
        List<TagsEntity> list;
        if (category != null && !category.isEmpty()) {
            list = tagsService.lambdaQuery()
                    .eq(TagsEntity::getStatus, 1)
                    .eq(TagsEntity::getTagCategory, category)
                    .orderByDesc(TagsEntity::getUsageCount)
                    .list();
        } else {
            list = tagsService.lambdaQuery()
                    .eq(TagsEntity::getStatus, 1)
                    .orderByDesc(TagsEntity::getUsageCount)
                    .last("LIMIT 50")
                    .list();
        }
        return new Result<List<TagsEntity>>().ok(list);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取话题分类列表")
    public Result<List<String>> categories() {
        List<String> categories = tagsService.getAllCategories();
        return new Result<List<String>>().ok(categories);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索话题")
    public Result<List<TagsEntity>> search(@RequestParam String keyword) {
        List<TagsEntity> list = tagsService.lambdaQuery()
                .eq(TagsEntity::getStatus, 1)
                .like(TagsEntity::getTagName, keyword)
                .orderByDesc(TagsEntity::getUsageCount)
                .last("LIMIT 20")
                .list();
        return new Result<List<TagsEntity>>().ok(list);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "话题详情")
    public Result<TagsEntity> detail(@PathVariable Long id) {
        TagsEntity tag = tagsService.getById(id);
        if (tag != null && tag.getStatus() == 1) {
            long followerCount = userTagsService.lambdaQuery()
                    .eq(UserTagsEntity::getTagId, tag.getId())
                    .eq(UserTagsEntity::getSourceType, FOLLOW_SOURCE_TYPE)
                    .count();
            tag.setFollowerCount((int) followerCount);
            return new Result<TagsEntity>().ok(tag);
        }
        return new Result<TagsEntity>().error("话题不存在");
    }

    @Login
    @PostMapping("/follow")
    @Operation(summary = "关注/取消关注话题")
    public Result<Void> follow(
            @Parameter(hidden = true) @LoginUser AppUserEntity user,
            @RequestBody Map<String, Object> body
    ) {
        Object tagIdObj = body.get("tagId");
        Object isFollowObj = body.get("isFollow");
        if (tagIdObj == null || isFollowObj == null) {
            return new Result<Void>().error("参数缺失");
        }
        Integer tagId = Integer.valueOf(tagIdObj.toString());
        Boolean isFollow = Boolean.valueOf(isFollowObj.toString());

        TagsEntity tag = tagsService.getById(tagId);
        if (tag == null || tag.getStatus() == null || tag.getStatus() != 1) {
            return new Result<Void>().error("话题不存在");
        }

        LambdaQueryWrapper<UserTagsEntity> followWrapper = new LambdaQueryWrapper<UserTagsEntity>()
                .eq(UserTagsEntity::getUserId, user.getUid())
                .eq(UserTagsEntity::getTagId, tagId)
                .eq(UserTagsEntity::getSourceType, FOLLOW_SOURCE_TYPE);

        boolean exists = userTagsService.count(followWrapper) > 0;
        if (isFollow) {
            if (!exists) {
                UserTagsEntity followTag = new UserTagsEntity();
                followTag.setUserId(user.getUid());
                followTag.setTagId(tagId);
                followTag.setSourceType(FOLLOW_SOURCE_TYPE);
                followTag.setWeight(BigDecimal.ONE);
                followTag.setCreateTime(new Date());
                userTagsService.save(followTag);
            }
        } else if (exists) {
            userTagsService.remove(followWrapper);
        }

        syncFollowerCount(tagId);
        return new Result<Void>().ok();
    }

    private void syncFollowerCount(Integer tagId) {
        long count = userTagsService.lambdaQuery()
                .eq(UserTagsEntity::getTagId, tagId)
                .eq(UserTagsEntity::getSourceType, FOLLOW_SOURCE_TYPE)
                .count();
        tagsService.lambdaUpdate()
                .eq(TagsEntity::getId, tagId)
                .setSql("follower_count = " + count)
                .update();
    }

    @GetMapping("/postsByTag")
    @Operation(summary = "获取话题下的帖子列表")
    public Result<Map<String, Object>> postsByTag(
            @RequestParam Integer tagId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(defaultValue = "latest") String sort // latest, hot
    ) {
        // 获取该话题下的所有帖子ID
        List<Integer> postIds = postTagService.lambdaQuery()
                .eq(PostTagEntity::getTagId, tagId)
                .list()
                .stream()
                .map(PostTagEntity::getPostId)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        if (postIds.isEmpty()) {
            result.put("list", Collections.emptyList());
            result.put("totalCount", 0);
            result.put("currentPage", page);
            result.put("pageSize", limit);
            result.put("totalPage", 0);
            return new Result<Map<String, Object>>().ok(result);
        }

        Page<PostEntity> pager = new Page<>(page, limit);
        IPage<PostEntity> postPage;
        if ("hot".equalsIgnoreCase(sort)) {
            postPage = postService.lambdaQuery()
                    .in(PostEntity::getId, postIds)
                    .eq(PostEntity::getStatus, 0)
                    .orderByDesc(PostEntity::getReadCount)
                    .orderByDesc(PostEntity::getCreateTime)
                    .page(pager);
        } else {
            postPage = postService.lambdaQuery()
                    .in(PostEntity::getId, postIds)
                    .eq(PostEntity::getStatus, 0)
                    .orderByDesc(PostEntity::getCreateTime)
                    .page(pager);
        }

        List<Map<String, Object>> list = postPage.getRecords().stream().map(post -> {
            PostDetailResponse detail = postService.detail(post.getId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", post.getId());
            item.put("uid", post.getUid());
            item.put("type", post.getType());
            item.put("title", post.getTitle());
            item.put("content", post.getContent());
            item.put("createTime", post.getCreateTime());

            if (detail != null) {
                item.put("images", detail.getMedia());
                item.put("likeCount", detail.getCollectionCount());
                item.put("commentCount", detail.getCommentCount());
                item.put("favoriteCount", detail.getCollectionCount());
                if (detail.getUserInfo() != null) {
                    item.put("avatar", detail.getUserInfo().getAvatar());
                    item.put("username", detail.getUserInfo().getUsername());
                }
            } else {
                item.put("images", Collections.emptyList());
                item.put("likeCount", 0);
                item.put("commentCount", 0);
                item.put("favoriteCount", 0);
                item.put("avatar", "");
                item.put("username", "用户");
            }
            return item;
        }).collect(Collectors.toList());

        result.put("list", list);
        result.put("totalCount", (int) postPage.getTotal());
        result.put("currentPage", page);
        result.put("pageSize", limit);
        result.put("totalPage", (int) postPage.getPages());

        return new Result<Map<String, Object>>().ok(result);
    }
}
