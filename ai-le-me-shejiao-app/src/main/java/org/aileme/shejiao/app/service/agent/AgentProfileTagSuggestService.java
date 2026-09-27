package org.aileme.shejiao.app.service.agent;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.TagsService;
import org.aileme.shejiao.app.service.TagProfileAggregateService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;
import org.aileme.shejiao.domain.vo.AgentTagSuggestionVO;
import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资料标签推荐服务（辅助决策）
 */
@Service
public class AgentProfileTagSuggestService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private TagsService tagsService;

    @Autowired
    private TagProfileAggregateService tagProfileAggregateService;

    public List<AgentTagSuggestionVO> suggest(Integer currentUid, Integer targetUid, Integer limit) {
        int safeLimit = Math.max(1, Math.min(10, limit == null ? 6 : limit));
        Integer profileUid = targetUid != null && targetUid > 0 ? targetUid : currentUid;
        if (profileUid == null || profileUid <= 0) {
            throw new LinfengException("用户未登录");
        }

        AppUserEntity user = appUserService.getById(profileUid);
        if (user == null) {
            throw new LinfengException("目标用户不存在");
        }

        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(profileUid);
        Set<String> existing = new HashSet<>(aggregate.getSelfTags());

        String profileText = String.join(" ", Arrays.asList(
                safeText(user.getSelfIntroduction()),
                safeText(user.getInterest()),
                safeText(user.getLoveDeclaration()),
                safeText(user.getIntro()),
                String.join(" ", aggregate.getBehaviorTags()),
                String.join(" ", aggregate.getImpressionTop())
        )).toLowerCase(Locale.ROOT);

        List<TagsEntity> options = tagsService.lambdaQuery()
                .eq(TagsEntity::getStatus, 1)
                .orderByDesc(TagsEntity::getUsageCount)
                .last("LIMIT 120")
                .list();

        List<TagCandidate> candidates = new ArrayList<>();
        for (TagsEntity option : options) {
            String tagName = StringUtils.trimToEmpty(option.getTagName());
            if (StringUtils.isBlank(tagName) || existing.contains(tagName)) {
                continue;
            }

            int score = 0;
            String reason = "基于资料文本推荐";
            String loweredTag = tagName.toLowerCase(Locale.ROOT);
            if (profileText.contains(loweredTag)) {
                score += 6;
                reason = "资料里多次出现相关表达";
            }
            if (containsAny(profileText, buildAlias(tagName))) {
                score += 3;
                reason = "聊天与资料关键词匹配";
            }
            if (aggregate.getImpressionTop().contains(tagName)) {
                score += 2;
                reason = "他人印象与你该标签高度相关";
            }
            if (aggregate.getBehaviorTags().contains(tagName)) {
                score += 2;
                reason = "近期行为中出现了该标签信号";
            }
            if (score <= 0) {
                continue;
            }
            candidates.add(new TagCandidate(tagName, score, reason));
        }

        if (candidates.isEmpty()) {
            candidates = fallbackByCategory(options, existing);
        }

        candidates.sort((a, b) -> Integer.compare(b.score, a.score));
        return candidates.stream()
                .limit(safeLimit)
                .map(item -> AgentTagSuggestionVO.builder()
                        .suggestionId("tag_" + UUID.randomUUID().toString().replace("-", ""))
                        .tag(item.tag)
                        .reason(item.reason)
                        .confidence(toConfidence(item.score))
                        .build())
                .collect(Collectors.toList());
    }

    private String safeText(String value) {
        return StringUtils.defaultString(value).trim();
    }

    private boolean containsAny(String text, List<String> aliases) {
        for (String alias : aliases) {
            if (StringUtils.isBlank(alias)) {
                continue;
            }
            if (text.contains(alias.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildAlias(String tagName) {
        List<String> aliases = new ArrayList<>();
        aliases.add(tagName);
        if (tagName.contains("运动")) aliases.addAll(Arrays.asList("跑步", "健身", "球类", "瑜伽"));
        if (tagName.contains("旅行")) aliases.addAll(Arrays.asList("出游", "citywalk", "徒步", "打卡"));
        if (tagName.contains("美食")) aliases.addAll(Arrays.asList("探店", "做饭", "烘焙"));
        if (tagName.contains("阅读")) aliases.addAll(Arrays.asList("看书", "写作", "思考"));
        if (tagName.contains("音乐")) aliases.addAll(Arrays.asList("唱歌", "吉他", "钢琴", "演唱会"));
        return aliases;
    }

    private List<TagCandidate> fallbackByCategory(List<TagsEntity> options, Set<String> existing) {
        List<TagCandidate> fallback = new ArrayList<>();
        for (TagsEntity option : options) {
            String tagName = StringUtils.trimToEmpty(option.getTagName());
            if (StringUtils.isBlank(tagName) || existing.contains(tagName)) {
                continue;
            }
            fallback.add(new TagCandidate(tagName, 1, "基于热门标签补充推荐"));
            if (fallback.size() >= 8) {
                break;
            }
        }
        return fallback;
    }

    private double toConfidence(int score) {
        double value = 0.45D + (score * 0.08D);
        if (value > 0.95D) {
            return 0.95D;
        }
        if (value < 0.3D) {
            return 0.3D;
        }
        return Math.round(value * 100D) / 100D;
    }

    private static class TagCandidate {
        private final String tag;
        private final int score;
        private final String reason;

        private TagCandidate(String tag, int score, String reason) {
            this.tag = tag;
            this.score = score;
            this.reason = reason;
        }
    }
}
