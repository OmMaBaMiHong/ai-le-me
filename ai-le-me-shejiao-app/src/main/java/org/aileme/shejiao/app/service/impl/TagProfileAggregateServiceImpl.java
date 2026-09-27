package org.aileme.shejiao.app.service.impl;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppTrackService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.api.service.UserImpressionTagService;
import org.aileme.shejiao.api.service.UserTagsService;
import org.aileme.shejiao.app.service.TagProfileAggregateService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;
import org.aileme.shejiao.domain.entity.admin.UserTagsEntity;
import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 标签画像聚合实现
 */
@Service("tagProfileAggregateService")
public class TagProfileAggregateServiceImpl implements TagProfileAggregateService {

    private static final int FOLLOW_SOURCE_TYPE = 4;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private UserImpressionTagService userImpressionTagService;

    @Autowired
    private UserTagsService userTagsService;

    @Autowired
    private TagsService tagsService;

    @Autowired
    private AppTrackService appTrackService;

    @Override
    public TagProfileAggregateVO aggregate(Integer userId) {
        TagProfileAggregateVO vo = new TagProfileAggregateVO();
        vo.setUserId(userId);
        if (userId == null || userId <= 0) {
            return vo;
        }

        AppUserEntity user = appUserService.getById(userId);
        if (user != null) {
            vo.setSelfTags(parseTagStr(user.getTagStr()));
        }

        Map<Integer, Integer> impressionMap = userImpressionTagService.getImpressionSummary(userId);
        fillImpressionTags(vo, impressionMap);
        vo.setFollowTopicTags(resolveFollowTopicTags(userId));

        LinkedHashSet<String> behaviorTags = new LinkedHashSet<>();
        behaviorTags.addAll(appTrackService.collectBehaviorTags(userId, 10));
        if (behaviorTags.isEmpty()) {
            behaviorTags.addAll(vo.getFollowTopicTags().stream().limit(4).collect(Collectors.toList()));
            behaviorTags.addAll(vo.getImpressionTop().stream().limit(3).collect(Collectors.toList()));
        }
        vo.setBehaviorTags(new ArrayList<>(behaviorTags).stream().limit(10).collect(Collectors.toList()));
        return vo;
    }

    private void fillImpressionTags(TagProfileAggregateVO vo, Map<Integer, Integer> impressionMap) {
        if (impressionMap == null || impressionMap.isEmpty()) {
            vo.setImpressionTop(Collections.emptyList());
            vo.setImpressionDetail(Collections.emptyList());
            return;
        }

        List<Map.Entry<Integer, Integer>> sorted = impressionMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<Integer> tagIds = sorted.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        Map<Integer, String> tagNameMap = tagsService.getBatchByIds(tagIds).stream()
                .collect(Collectors.toMap(TagsEntity::getId, TagsEntity::getTagName, (a, b) -> a));

        List<String> impressionTop = new ArrayList<>();
        List<TagProfileAggregateVO.TagCountVO> details = new ArrayList<>();
        for (Map.Entry<Integer, Integer> item : sorted) {
            String tagName = StringUtils.trimToEmpty(tagNameMap.get(item.getKey()));
            if (StringUtils.isBlank(tagName)) {
                continue;
            }
            if (impressionTop.size() < 8) {
                impressionTop.add(tagName);
            }
            TagProfileAggregateVO.TagCountVO detail = new TagProfileAggregateVO.TagCountVO();
            detail.setTag(tagName);
            detail.setCount(item.getValue());
            details.add(detail);
        }
        vo.setImpressionTop(impressionTop);
        vo.setImpressionDetail(details);
    }

    private List<String> resolveFollowTopicTags(Integer userId) {
        List<UserTagsEntity> follows = userTagsService.lambdaQuery()
                .eq(UserTagsEntity::getUserId, userId)
                .eq(UserTagsEntity::getSourceType, FOLLOW_SOURCE_TYPE)
                .orderByDesc(UserTagsEntity::getCreateTime)
                .list();

        if (follows == null || follows.isEmpty()) {
            follows = userTagsService.lambdaQuery()
                    .eq(UserTagsEntity::getUserId, userId)
                    .orderByDesc(UserTagsEntity::getCreateTime)
                    .last("LIMIT 20")
                    .list();
        }
        if (follows == null || follows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> tagIds = follows.stream()
                .map(UserTagsEntity::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, String> tagNameMap = tagsService.getBatchByIds(tagIds).stream()
                .collect(Collectors.toMap(TagsEntity::getId, TagsEntity::getTagName, (a, b) -> a));

        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Integer tagId : tagIds) {
            String name = StringUtils.trimToEmpty(tagNameMap.get(tagId));
            if (StringUtils.isNotBlank(name)) {
                result.add(name);
            }
            if (result.size() >= 10) {
                break;
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> parseTagStr(String tagStr) {
        if (StringUtils.isBlank(tagStr)) {
            return Collections.emptyList();
        }
        String raw = tagStr.trim();
        if (raw.startsWith("[")) {
            try {
                List<String> parsed = JSON.parseArray(raw, String.class);
                if (parsed != null) {
                    return parsed.stream().filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
                }
            } catch (Exception ignored) {
                raw = raw.substring(1, Math.max(1, raw.length() - 1));
            }
        }
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
