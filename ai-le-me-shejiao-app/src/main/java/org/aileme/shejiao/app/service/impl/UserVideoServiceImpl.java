package org.aileme.shejiao.app.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.common.oss.factory.OssFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.api.service.*;
import org.aileme.shejiao.app.dao.UserVideoDao;
import org.aileme.shejiao.app.service.ai.video.AIVideoProvider;
import org.aileme.shejiao.app.service.ai.video.AIVideoStrategyManager;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.JsonUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.entity.admin.TopicEntity;
import org.aileme.shejiao.domain.entity.app.UserVideoEntity;
import org.aileme.shejiao.domain.entity.app.VideoTemplateEntity;
import org.aileme.shejiao.domain.param.app.GenerateVideoForm;
import org.aileme.shejiao.domain.param.app.PublishVideoForm;
import org.aileme.shejiao.domain.vo.*;
import org.aileme.shejiao.gateway.runtime.ThirdPartyResolvedRoute;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户AI视频服务实现
 *
 * @author system
 * @date 2026-02-13
 */
@Slf4j
@DS("master")
@Service("userVideoService")
public class UserVideoServiceImpl extends ServiceImpl<UserVideoDao, UserVideoEntity> implements UserVideoService {

    private static final int VIDEO_ERROR_MSG_MAX_LENGTH = 500;
    private static final DateTimeFormatter TOS_SIGNED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    @Autowired
    private VideoTemplateService videoTemplateService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private PostService postService;

    @Autowired
    private SysConfigService configService;

    @Autowired
    private AIVideoStrategyManager videoStrategyManager;

    @Value("${shejiao.ai-video.enabled:true}")
    private boolean aiVideoEnabled;

    @Override
    public List<VideoTemplateVo> getAvailableTemplates(String category) {
        var query = this.videoTemplateService.lambdaQuery()
                .eq(VideoTemplateEntity::getStatus, VideoTemplateEntity.STATUS_ENABLED);
        if (category != null && !category.isEmpty()) {
            query.eq(VideoTemplateEntity::getCategory, category);
        }
        List<VideoTemplateEntity> templates = query
                .orderByAsc(VideoTemplateEntity::getSort)
                .list();

        return templates.stream().map(template -> {
            VideoTemplateVo vo = new VideoTemplateVo();
            BeanUtils.copyProperties(template, vo);
            vo.setCategoryName(getCategoryName(template.getCategory()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getTemplateCategories() {
        List<VideoTemplateEntity> templates = this.videoTemplateService.lambdaQuery()
                .eq(VideoTemplateEntity::getStatus, VideoTemplateEntity.STATUS_ENABLED)
                .select(VideoTemplateEntity::getCategory)
                .list();
        return templates.stream()
                .map(VideoTemplateEntity::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .map(c -> {
                    Map<String, String> map = new java.util.LinkedHashMap<>();
                    map.put("category", c);
                    map.put("categoryName", getCategoryName(c));
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 分类编码转中文名称
     */
    private String getCategoryName(String category) {
        if (category == null) return "未分类";
        switch (category) {
            case "costume_change": return "汉服换装";
            case "cosplay": return "动漫 Coser";
            case "cinematic_portrait": return "胶片妆造";
            case "fantasy_transform": return "妖系变身";
            case "growth_story": return "成长故事";
            case "self_intro": return "自我介绍";
            case "dating": return "约会邀请";
            case "confession": return "表白/追求";
            case "daily": return "日常分享";
            default: return category;
        }
    }

    @Override
    @DSTransactional
    public VideoGenerateVo generateVideo(Integer uid, GenerateVideoForm form) {
        // 1. 检查功能是否开启
        if (!aiVideoEnabled) {
            throw new LinfengException("AI视频生成功能暂未开放");
        }

        // 2. 获取用户信息
        AppUserEntity user = appUserService.getById(uid);
        if (user == null) {
            throw new LinfengException("用户不存在");
        }

        // 3. 获取模板
        VideoTemplateEntity template = videoTemplateService.getByCode(form.getTemplateCode());
        if (template == null) {
            throw new LinfengException("视频模板不存在");
        }

        // 4. 构建提示词
        String prompt = form.getCustomPrompt();
        if (StringUtils.isBlank(prompt)) {
            prompt = buildPrompt(user, template);
        }

        // 5. 收集素材图片
        List<String> images = collectImages(user, form.getMedia(), template.getMaxImages());
        if (images.isEmpty()) {
            throw new LinfengException("请先上传真人头像或生活照后再生成");
        }

        // 6. 收集素材文字
        Map<String, Object> sourceText = buildSourceText(user, template);

        Map<String, String> routeContext = buildVideoRouteContext(template, images);
        ThirdPartyResolvedRoute route = videoStrategyManager.resolveRoute(routeContext);
        String routeKey = videoStrategyManager.buildRouteKey(route);

        // 7. 创建视频记录
        UserVideoEntity video = new UserVideoEntity();
        video.setUid(uid);
        video.setVideoType(resolveVideoType(template));
        video.setTitle(buildVideoTitle(user, template));
        video.setTemplateCode(form.getTemplateCode());
        video.setPromptText(prompt);
        video.setSourceImages(JSON.toJSONString(images));
        video.setSourceText(JSON.toJSONString(sourceText));
        video.setAiModel(routeKey);
        video.setStatus(UserVideoEntity.STATUS_GENERATING);
        video.setProgress(0);
        video.setRetryCount(0);
        video.setAutoPublish(form.getAutoPublish());
        video.setPostContent(buildSuggestedPostContent(user, template, form.getPostContent()));
        video.setCreateTime(DateUtil.nowDateTime());
        video.setUpdateTime(DateUtil.nowDateTime());

        this.save(video);

        // 8. 提交AI生成任务（这里需要调用实际的AI服务）
        // TODO: 实际调用AI视频生成API
        submitAITask(video.getId(), prompt, images, template, routeContext, route);

        return VideoGenerateVo.builder()
                .videoId(video.getId())
                .estimatedTime(60)
                .prompt(prompt)
                .build();
    }

    @Override
    public VideoStatusVo getVideoStatus(Integer videoId, Integer uid) {
        UserVideoEntity video = this.getPlayableVideoById(videoId);
        if (video == null || !video.getUid().equals(uid)) {
            throw new LinfengException("视频不存在");
        }
    
        // 直接返回数据库中的状态(由后台轮询线程更新)
        return VideoStatusVo.builder()
                .status(video.getStatus())
                .progress(video.getProgress())
                .videoUrl(video.getVideoUrl())
                .coverUrl(video.getCoverUrl())
                .duration(video.getDuration())
                .errorMsg(video.getErrorMsg())
                .build();
    }

    @Override
    @DSTransactional
    public Integer publishVideo(Integer videoId, Integer uid, PublishVideoForm form) {
        // 1. 校验视频
        UserVideoEntity video = this.getPlayableVideoById(videoId);
        if (video == null || !video.getUid().equals(uid)) {
            throw new LinfengException("视频不存在");
        }
        if (video.getStatus() != UserVideoEntity.STATUS_READY) {
            throw new LinfengException("视频状态异常，无法发布");
        }
        if (StringUtils.isBlank(video.getVideoUrl())) {
            throw new LinfengException("视频URL为空，无法发布");
        }

        // 2. 获取圈子ID
        Integer topicId = form.getTopicId();
        if (topicId == null) {
            topicId = getDefaultTopicId();
        }

        // 3. 构建发布内容
        String content = form.getContent();
        if (StringUtils.isBlank(content)) {
            content = video.getPostContent();
        }
        if (StringUtils.isBlank(content)) {
            content = buildDefaultContent(video);
        }

        // 4. 创建帖子
        PostEntity post = new PostEntity();
        post.setUid(uid);
        post.setTopicId(topicId);
        post.setType(2); // 视频类型
        post.setTitle(video.getTitle());
        post.setContent(content);
        String normalizedVideoUrl = JsonUtils.normalizeUrlValue(video.getVideoUrl());
        if (StringUtils.isBlank(normalizedVideoUrl)) {
            throw new LinfengException("视频地址异常，暂时无法发布");
        }
        post.setMedia(JSON.toJSONString(Collections.singletonList(normalizedVideoUrl)));
        post.setIsPrivate(0); // 公开
        post.setCut(0); // 普通贴
        post.setCreateTime(DateUtil.nowDateTime());
        post.setStatus(0); // 正常状态
        post.setIsAiVideo(1); // 标记为AI生成
        post.setVideoId(videoId);

        postService.save(post);

        // 5. 更新视频状态
        video.setPostId(post.getId());
        video.setStatus(UserVideoEntity.STATUS_PUBLISHED);
        video.setUpdateTime(DateUtil.nowDateTime());
        this.updateById(video);

        return post.getId();
    }

    @Override
    public List<UserVideoVo> getUserVideoList(Integer uid, Integer page, Integer limit) {
        int offset = (page - 1) * limit;
        List<UserVideoEntity> videos = this.lambdaQuery()
                .eq(UserVideoEntity::getUid, uid)
                .orderByDesc(UserVideoEntity::getCreateTime)
                .last("LIMIT " + offset + ", " + limit)
                .list();

        return videos.stream()
                .map(this::refreshVideoAccessIfNeeded)
                .filter(Objects::nonNull)
                .map(video -> {
            UserVideoVo vo = new UserVideoVo();
            BeanUtils.copyProperties(video, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserVideoEntity> getPlayableVideos(Collection<Integer> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return videoIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(this::getPlayableVideoById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public UserVideoEntity getPlayableVideoById(Integer videoId) {
        if (videoId == null) {
            return null;
        }
        return refreshVideoAccessIfNeeded(this.getById(videoId));
    }

    @Override
    public UserVideoVo getVideoDetail(Integer videoId, Integer uid) {
        UserVideoEntity video = this.getPlayableVideoById(videoId);
        if (video == null || !video.getUid().equals(uid)) {
            throw new LinfengException("视频不存在");
        }

        UserVideoVo vo = new UserVideoVo();
        BeanUtils.copyProperties(video, vo);
        return vo;
    }

    @Override
    @DSTransactional
    public void deleteVideo(Integer videoId, Integer uid) {
        UserVideoEntity video = this.getById(videoId);
        if (video == null || !video.getUid().equals(uid)) {
            throw new LinfengException("视频不存在");
        }

        // 如果已发布，需要同时删除帖子
        if (video.getPostId() != null) {
            postService.removeById(video.getPostId());
        }

        this.removeById(videoId);
    }

    @Override
    @DSTransactional
    public void handleGenerateCallback(Integer videoId, String videoUrl, String coverUrl, Integer duration) {
        UserVideoEntity video = this.getById(videoId);
        if (video == null) {
            log.warn("视频记录不存在: {}", videoId);
            return;
        }

        String persistedVideoUrl = persistRemoteMediaIfNecessary(videoUrl, "mp4", videoId, "video");
        String persistedCoverUrl = persistRemoteMediaIfNecessary(coverUrl, "jpg", videoId, "cover");

        video.setVideoUrl(JsonUtils.normalizeUrlValue(persistedVideoUrl));
        video.setCoverUrl(JsonUtils.normalizeUrlValue(persistedCoverUrl));
        video.setDuration(duration);
        video.setProgress(100);
        video.setStatus(UserVideoEntity.STATUS_READY);
        video.setUpdateTime(DateUtil.nowDateTime());
        this.updateById(video);

        // 如果设置了自动发布
        if (video.getAutoPublish() != null && video.getAutoPublish() == 1) {
            try {
                publishVideo(videoId, video.getUid(), new PublishVideoForm());
            } catch (Exception e) {
                log.error("自动发布失败: {}", e.getMessage());
                video.setStatus(UserVideoEntity.STATUS_READY);
                video.setUpdateTime(DateUtil.nowDateTime());
                this.updateById(video);
            }
        }
        syncPublishedPostMedia(video, video.getVideoUrl());
    }

    @Override
    @DSTransactional
    public void handleGenerateError(Integer videoId, String errorMsg) {
        UserVideoEntity video = this.getById(videoId);
        if (video == null) {
            return;
        }

        video.setStatus(UserVideoEntity.STATUS_FAILED);
        video.setErrorMsg(truncateErrorMessage(errorMsg));
        video.setRetryCount((video.getRetryCount() == null ? 0 : video.getRetryCount()) + 1);
        video.setUpdateTime(DateUtil.nowDateTime());
        this.updateById(video);
    }

    @Override
    public void updateProgress(Integer videoId, Integer progress) {
        UserVideoEntity video = this.getById(videoId);
        if (video != null) {
            video.setProgress(progress);
            video.setUpdateTime(DateUtil.nowDateTime());
            this.updateById(video);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 构建提示词
     */
    private String buildPrompt(AppUserEntity user, VideoTemplateEntity template) {
        Map<String, String> profile = buildProfileMap(user);
        String promptTemplate = StringUtils.defaultIfBlank(
                template.getPromptTemplate(),
                "围绕{username}在{city}的真实生活展开，展示{job}身份、{interest}爱好与{loveDeclaration}。"
        );

        String coreMessage = replacePlaceholders(promptTemplate, profile);
        String summary = String.format(
                "%s，%s岁，%s，常住%s，职业%s，%s，身高%s。",
                profile.get("username"),
                profile.get("age"),
                profile.get("gender"),
                profile.get("city"),
                profile.get("job"),
                profile.get("education"),
                profile.get("height")
        );

        List<String> beats = buildSceneBeats(template.getCategory(), profile);

        return String.join("\n",
                "请生成一支适合婚恋交友平台发布的真人质感竖屏短视频。",
                "视频主题：" + getCategoryName(template.getCategory()) + "。",
                "视频风格：" + getStyleLabel(template.getStylePreset()) + "。",
                "人物设定：" + summary,
                "人物关键词：" + profile.get("tags") + "；兴趣爱好：" + profile.get("interest") + "。",
                "核心表达：" + coreMessage,
                "镜头节奏：" + String.join("；", beats),
                "画面要求：9:16 竖屏，暖色自然光，真人写实，表情自然，轻电影感，字幕简洁，不要夸张特效，不要卡通化。",
                "交付目标：让观看者快速感受到真诚、稳定和愿意认真进入关系的态度。"
        );
    }

    /**
     * 收集素材图片
     */
    private List<String> collectImages(AppUserEntity user, List<String> preferredImages, Integer maxImages) {
        int imageLimit = maxImages == null || maxImages <= 0 ? 3 : maxImages;
        LinkedHashSet<String> images = new LinkedHashSet<>();

        appendImages(images, preferredImages, imageLimit);
        if (images.size() < imageLimit) {
            appendImages(images, Collections.singletonList(user.getAvatar()), imageLimit);
        }
        if (images.size() < imageLimit) {
            try {
                appendImages(images, parseFigureImages(user.getFigur()), imageLimit);
            } catch (Exception e) {
                log.warn("解析生活照失败: {}", e.getMessage());
            }
        }

        return new ArrayList<>(images);
    }

    private void appendImages(Set<String> target, List<String> candidates, int limit) {
        if (candidates == null || candidates.isEmpty() || target.size() >= limit) {
            return;
        }
        for (String item : candidates) {
            if (!isUsableProfileImage(item)) {
                continue;
            }
            target.add(item.trim());
            if (target.size() >= limit) {
                break;
            }
        }
    }

    private List<String> parseFigureImages(String figur) {
        if (StringUtils.isBlank(figur)) {
            return Collections.emptyList();
        }
        String raw = figur.trim();
        if (raw.startsWith("[")) {
            List<String> list = JSON.parseArray(raw, String.class);
            return list == null ? Collections.emptyList() : list;
        }
        String[] split = raw.split("\\s*,\\s*");
        List<String> list = new ArrayList<>(split.length);
        for (String item : split) {
            if (StringUtils.isNotBlank(item)) {
                list.add(item.trim());
            }
        }
        return list;
    }

    private boolean isUsableProfileImage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        return !StringUtils.containsAny(normalized,
                "/static/default-avatar.png",
                "/static/images/default-avatar.png",
                "/static/images/unlogin_avatar.png",
                "default-avatar",
                "default_avatar",
                "avatar-default");
    }

    /**
     * 构建素材文字
     */
    private Map<String, Object> buildSourceText(AppUserEntity user, VideoTemplateEntity template) {
        Map<String, Object> text = new HashMap<>();
        text.put("username", user.getUsername());
        text.put("gender", user.getGender());
        text.put("age", user.getAge());
        text.put("city", user.getAbodeCity());
        text.put("job", user.getJob());
        text.put("education", user.getEducation());
        text.put("height", user.getHeight());
        text.put("selfIntroduction", user.getSelfIntroduction());
        text.put("interest", user.getInterest());
        text.put("loveDeclaration", user.getLoveDeclaration());
        text.put("tags", user.getTagStr());
        text.put("category", template.getCategory());
        text.put("categoryName", getCategoryName(template.getCategory()));
        text.put("stylePreset", template.getStylePreset());
        text.put("templateName", template.getName());
        return text;
    }

    /**
     * 获取学历文本
     */
    private String getEducationText(Integer education) {
        if (education == null) return "本科";
        switch (education) {
            case 1: return "高中";
            case 2: return "大专";
            case 3: return "本科";
            case 4: return "硕士";
            case 5: return "博士";
            default: return "本科";
        }
    }

    /**
     * 获取默认圈子ID
     */
    private Integer getDefaultTopicId() {
        String defaultTopicId = configService.getValue("ai_video_default_topic");
        if (StringUtils.isNotBlank(defaultTopicId)) {
            return Integer.valueOf(defaultTopicId);
        }

        // 返回第一个公开圈子
        TopicEntity topic = topicService.lambdaQuery()
                .eq(TopicEntity::getIsPrivacy, 0)
                .last("LIMIT 1")
                .one();

        return topic != null ? topic.getId() : 1;
    }

    /**
     * 构建默认发布文案
     */
    private String buildDefaultContent(UserVideoEntity video) {
        String template = switch (video.getVideoType()) {
            case UserVideoEntity.TYPE_DATING_CARD -> "这是我的相亲名片，期待遇见对的你 💕";
            default -> "这是我的自我介绍视频，让更多人认识真实的我 ✨";
        };
        return template;
    }

    private Integer resolveVideoType(VideoTemplateEntity template) {
        if (template == null) {
            return UserVideoEntity.TYPE_SELF_INTRO;
        }
        return StringUtils.equalsAnyIgnoreCase(template.getCategory(), "dating", "confession")
                ? UserVideoEntity.TYPE_DATING_CARD
                : UserVideoEntity.TYPE_SELF_INTRO;
    }

    private String buildVideoTitle(AppUserEntity user, VideoTemplateEntity template) {
        String username = StringUtils.defaultIfBlank(user.getUsername(), "我");
        String categoryName = getCategoryName(template.getCategory());
        return switch (StringUtils.defaultIfBlank(template.getCategory(), "self_intro")) {
            case "dating" -> username + "的约会邀请短片";
            case "confession" -> username + "的真诚告白";
            case "daily" -> username + "的日常魅力片段";
            default -> username + "的" + categoryName + "视频";
        };
    }

    private String buildSuggestedPostContent(AppUserEntity user, VideoTemplateEntity template, String providedContent) {
        if (StringUtils.isNotBlank(providedContent)) {
            return providedContent;
        }

        String city = firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity(), "同城");
        String username = StringUtils.defaultIfBlank(user.getUsername(), "我");

        return switch (StringUtils.defaultIfBlank(template.getCategory(), "self_intro")) {
            case "dating" -> username + "在" + city + "，认真找对象，也认真期待一次舒服的见面。";
            case "confession" -> "如果你也喜欢真诚稳定的关系，希望这支视频能让你更了解" + username + "。";
            case "daily" -> username + "把生活里真实的一面放进了视频里，欢迎来认识。";
            default -> "这是" + username + "的 AI 自我介绍短片，想把真实的自己更自然地展示出来。";
        };
    }

    private Map<String, String> buildProfileMap(AppUserEntity user) {
        Map<String, String> profile = new HashMap<>();
        profile.put("gender", user.getGender() != null && user.getGender() == 2 ? "女" : "男");
        profile.put("age", user.getAge() != null ? String.valueOf(user.getAge()) : "25");
        profile.put("city", firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity(), "北京"));
        profile.put("height", StringUtils.isNotBlank(user.getHeight()) ? user.getHeight() + "cm" : "175cm");
        profile.put("education", getEducationText(user.getEducation()));
        profile.put("tags", StringUtils.defaultIfBlank(user.getTagStr(), "真诚、温和、好相处"));
        profile.put("interest", StringUtils.defaultIfBlank(user.getInterest(), "运动、阅读、旅行"));
        profile.put("selfIntro", StringUtils.defaultIfBlank(user.getSelfIntroduction(), "希望通过一支短片介绍真实的自己。"));
        profile.put("loveDeclaration", StringUtils.defaultIfBlank(user.getLoveDeclaration(), "期待遇见稳定真诚、愿意认真相处的人。"));
        profile.put("job", StringUtils.defaultIfBlank(user.getJob(), "自由职业"));
        profile.put("username", StringUtils.defaultIfBlank(user.getUsername(), "我"));
        return profile;
    }

    private String replacePlaceholders(String template, Map<String, String> profile) {
        String result = template;
        for (Map.Entry<String, String> entry : profile.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", StringUtils.defaultIfBlank(entry.getValue(), ""));
        }
        return result;
    }

    private List<String> buildSceneBeats(String category, Map<String, String> profile) {
        List<String> beats = new ArrayList<>();
        String city = profile.get("city");
        String username = profile.get("username");
        switch (StringUtils.defaultIfBlank(category, "self_intro")) {
            case "dating":
                beats.add("开场用城市街景或咖啡馆氛围，展示" + username + "轻松微笑地走近镜头");
                beats.add("中段用生活兴趣和工作状态镜头，传递稳定、会生活、有分寸的感觉");
                beats.add("结尾给出真诚邀约感，让人愿意在" + city + "开启第一次见面");
                break;
            case "confession":
                beats.add("开场用近景眼神和自然笑容建立真实感");
                beats.add("中段穿插日常细节镜头，表达重视关系与长期投入的态度");
                beats.add("结尾用一句温柔而坚定的话收束，不煽情但有分量");
                break;
            case "daily":
                beats.add("开场展示在" + city + "的日常状态，轻松自然");
                beats.add("中段切换兴趣爱好、步行或工作状态镜头，保持节奏明快");
                beats.add("结尾停留在真实笑容和温暖氛围，突出可接近感");
                break;
            default:
                beats.add("开场用半身近景介绍" + username + "，快速建立信任感");
                beats.add("中段展示在" + city + "的生活场景、兴趣爱好和职业状态");
                beats.add("结尾强调真诚稳定、愿意认真恋爱的态度，给人想继续了解的感觉");
                break;
        }
        return beats;
    }

    private String getStyleLabel(String stylePreset) {
        return switch (StringUtils.defaultIfBlank(stylePreset, "natural")) {
            case "romantic" -> "浪漫心动";
            case "energetic" -> "轻快有活力";
            case "elegant" -> "克制高级";
            default -> "自然真实";
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String truncateErrorMessage(String errorMsg) {
        if (StringUtils.isBlank(errorMsg)) {
            return "AI 视频生成失败";
        }
        return errorMsg.length() <= VIDEO_ERROR_MSG_MAX_LENGTH
                ? errorMsg
                : errorMsg.substring(0, VIDEO_ERROR_MSG_MAX_LENGTH - 3) + "...";
    }

    /**
     * 提交AI生成任务
     */
    private void submitAITask(Integer videoId,
                              String prompt,
                              List<String> images,
                              VideoTemplateEntity template,
                              Map<String, String> routeContext,
                              ThirdPartyResolvedRoute resolvedRoute) {
        log.info("提交AI视频生成任务: videoId={}, prompt={}", videoId, prompt);
        
        try {
            // 构建额外参数
            Map<String, Object> params = new HashMap<>();
            
            // 从模板获取配置(解析durationRange，取最小值)
            if (StringUtils.isNotBlank(template.getDurationRange())) {
                try {
                    String[] parts = template.getDurationRange().split("-");
                    if (parts.length > 0) {
                        int duration = Integer.parseInt(parts[0].trim());
                        params.put("duration", duration);
                    }
                } catch (Exception e) {
                    log.warn("解析时长失败: {}", template.getDurationRange());
                    params.put("duration", 5); // 默认5秒
                }
            } else {
                params.put("duration", 5); // 默认5秒
            }
            
            // 设置默认宽高比(根据场景判断)
            String aspectRatio = "9:16"; // 默认竖屏(适合移动端)
            params.put("aspect_ratio", aspectRatio);

            Map<String, String> currentRouteContext = routeContext == null
                    ? buildVideoRouteContext(template, images)
                    : new LinkedHashMap<>(routeContext);
            ThirdPartyResolvedRoute route = resolvedRoute != null
                    ? resolvedRoute
                    : videoStrategyManager.resolveRoute(currentRouteContext);
            AIVideoProvider provider = videoStrategyManager.getProviderForRoute(route);
            String routeKey = videoStrategyManager.buildRouteKey(route);

            // 调用AI服务创建任务
            String taskId = provider.createVideoTask(prompt, images, params);
            
            // 保存任务ID和路由信息到数据库，避免后续切换配置造成状态查询错位
            UserVideoEntity video = this.getById(videoId);
            if (video != null) {
                video.setTaskId(taskId);
                video.setProgress(10);
                video.setAiModel(routeKey);
                this.updateById(video);
            }
            
            log.info("AI视频任务创建成功: videoId={}, taskId={}, routeKey={}, ruleId={}, routeContext={}",
                    videoId,
                    taskId,
                    routeKey,
                    route.getRouteRuleId(),
                    currentRouteContext);
            // 轮询逻辑已迁移到 Quartz 任务，不再使用原始线程
            
        } catch (Exception e) {
            log.error("AI视频任务创建失败", e);
            // 更新为失败状态
            handleGenerateError(videoId, e.getMessage());
        }
    }

    private Map<String, String> buildVideoRouteContext(VideoTemplateEntity template, List<String> images) {
        Map<String, String> context = new LinkedHashMap<>();
        if (template != null) {
            context.put("template_code", StringUtils.trimToEmpty(template.getCode()));
            context.put("scene_code", StringUtils.trimToEmpty(template.getCategory()));
            context.put("style_preset", StringUtils.trimToEmpty(template.getStylePreset()));
        }
        context.put("function_type", "video_generate");
        context.put("content_mode", images != null && !images.isEmpty() ? "i2v" : "t2v");
        return context;
    }

    private UserVideoEntity refreshVideoAccessIfNeeded(UserVideoEntity video) {
        if (video == null) {
            return null;
        }

        String currentVideoUrl = JsonUtils.normalizeUrlValue(video.getVideoUrl());
        String currentCoverUrl = JsonUtils.normalizeUrlValue(video.getCoverUrl());
        boolean needsMigration = needsPersistentMigration(currentVideoUrl) || needsPersistentMigration(currentCoverUrl);
        if (!needsMigration) {
            return video;
        }

        String candidateVideoUrl = currentVideoUrl;
        String candidateCoverUrl = currentCoverUrl;
        Integer duration = video.getDuration();

        if (isExpiredTemporaryUrl(candidateVideoUrl) || StringUtils.isBlank(candidateVideoUrl)) {
            AIVideoProvider.VideoTaskStatus taskStatus = queryLatestTaskStatus(video);
            if (taskStatus != null) {
                candidateVideoUrl = StringUtils.defaultIfBlank(JsonUtils.normalizeUrlValue(taskStatus.getVideoUrl()), candidateVideoUrl);
                candidateCoverUrl = StringUtils.defaultIfBlank(JsonUtils.normalizeUrlValue(taskStatus.getCoverUrl()), candidateCoverUrl);
                if (taskStatus.getDuration() != null) {
                    duration = taskStatus.getDuration();
                }
            }
        }

        String persistedVideoUrl = persistRemoteMediaIfNecessary(candidateVideoUrl, "mp4", video.getId(), "video");
        String persistedCoverUrl = persistRemoteMediaIfNecessary(candidateCoverUrl, "jpg", video.getId(), "cover");

        boolean changed =
                !StringUtils.equals(currentVideoUrl, persistedVideoUrl) ||
                !StringUtils.equals(currentCoverUrl, persistedCoverUrl) ||
                !Objects.equals(video.getDuration(), duration);
        if (!changed) {
            return video;
        }

        video.setVideoUrl(persistedVideoUrl);
        video.setCoverUrl(persistedCoverUrl);
        video.setDuration(duration);
        video.setUpdateTime(DateUtil.nowDateTime());
        this.updateById(video);
        syncPublishedPostMedia(video, persistedVideoUrl);
        return this.getById(video.getId());
    }

    private AIVideoProvider.VideoTaskStatus queryLatestTaskStatus(UserVideoEntity video) {
        if (video == null || StringUtils.isBlank(video.getTaskId())) {
            return null;
        }
        try {
            String providerCode = StringUtils.defaultIfBlank(video.getAiModel(), videoStrategyManager.getCurrentProviderCode());
            AIVideoProvider.VideoTaskStatus taskStatus = videoStrategyManager.queryTaskStatus(video.getTaskId(), providerCode);
            if (taskStatus == null || StringUtils.isBlank(taskStatus.getVideoUrl())) {
                return null;
            }
            return taskStatus;
        } catch (Exception e) {
            log.warn("刷新豆包视频地址失败: videoId={}, taskId={}, error={}", video.getId(), video.getTaskId(), e.getMessage());
            return null;
        }
    }

    private String persistRemoteMediaIfNecessary(String rawUrl, String fallbackSuffix, Integer videoId, String kind) {
        String normalizedUrl = JsonUtils.normalizeUrlValue(rawUrl);
        if (StringUtils.isBlank(normalizedUrl)) {
            return normalizedUrl;
        }
        String lowerUrl = normalizedUrl.toLowerCase(Locale.ROOT);
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return normalizedUrl;
        }
        if (!needsPersistentMigration(normalizedUrl)) {
            return normalizedUrl;
        }
        try {
            URLConnection connection = URI.create(normalizedUrl).toURL().openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(120000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            String contentType = connection.getContentType();
            byte[] mediaBytes;
            try (InputStream inputStream = connection.getInputStream()) {
                mediaBytes = inputStream.readAllBytes();
            }
            if (mediaBytes.length == 0) {
                throw new IllegalStateException("empty body");
            }
            String suffix = resolveFileSuffix(normalizedUrl, contentType, fallbackSuffix);
            String persistedUrl = OssFactory.instance()
                    .uploadSuffix(mediaBytes, suffix, resolveContentType(contentType, suffix))
                    .getUrl();
            String persistedNormalizedUrl = JsonUtils.normalizeUrlValue(persistedUrl);
            log.info("AI视频资源已转存OSS: videoId={}, kind={}, sourceUrl={}, persistedUrl={}",
                    videoId, kind, normalizedUrl, persistedNormalizedUrl);
            return persistedNormalizedUrl;
        } catch (Exception e) {
            log.warn("AI视频资源转存OSS失败，暂保留源地址: videoId={}, kind={}, url={}, error={}",
                    videoId, kind, normalizedUrl, e.getMessage());
            return normalizedUrl;
        }
    }

    private String resolveContentType(String contentType, String suffix) {
        String normalizedType = StringUtils.trimToEmpty(contentType);
        if (StringUtils.isNotBlank(normalizedType)) {
            return normalizedType;
        }
        String guessedType = URLConnection.guessContentTypeFromName("file." + StringUtils.defaultIfBlank(suffix, ""));
        if (StringUtils.isNotBlank(guessedType)) {
            return guessedType;
        }
        if (StringUtils.equalsAnyIgnoreCase(suffix, "mp4", "mov", "m4v")) {
            return "video/mp4";
        }
        if (StringUtils.equalsAnyIgnoreCase(suffix, "png")) {
            return "image/png";
        }
        if (StringUtils.equalsAnyIgnoreCase(suffix, "webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String resolveFileSuffix(String url, String contentType, String fallbackSuffix) {
        String normalizedUrl = StringUtils.defaultString(url);
        int queryIndex = normalizedUrl.indexOf('?');
        String path = queryIndex >= 0 ? normalizedUrl.substring(0, queryIndex) : normalizedUrl;
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < path.length() - 1) {
            return path.substring(dotIndex + 1);
        }
        String normalizedContentType = StringUtils.defaultString(contentType).toLowerCase(Locale.ROOT);
        if (normalizedContentType.contains("mp4")) {
            return "mp4";
        }
        if (normalizedContentType.contains("jpeg") || normalizedContentType.contains("jpg")) {
            return "jpg";
        }
        if (normalizedContentType.contains("png")) {
            return "png";
        }
        return fallbackSuffix;
    }

    private boolean needsPersistentMigration(String url) {
        String normalizedUrl = StringUtils.defaultString(url);
        return normalizedUrl.contains("X-Tos-Algorithm=")
                || normalizedUrl.contains("X-Tos-Signature=")
                || normalizedUrl.contains("volces.com");
    }

    private boolean isExpiredTemporaryUrl(String url) {
        String normalizedUrl = StringUtils.defaultString(url);
        if (!normalizedUrl.contains("X-Tos-Date=") || !normalizedUrl.contains("X-Tos-Expires=")) {
            return false;
        }
        try {
            Map<String, String> queryParams = parseQueryParams(normalizedUrl);
            String signedAt = queryParams.get("X-Tos-Date");
            String expires = queryParams.get("X-Tos-Expires");
            if (StringUtils.isBlank(signedAt) || StringUtils.isBlank(expires)) {
                return false;
            }
            LocalDateTime issueAt = LocalDateTime.parse(signedAt, TOS_SIGNED_AT_FORMATTER);
            long expiresSeconds = Long.parseLong(expires);
            Instant expireAt = issueAt.toInstant(ZoneOffset.UTC).plusSeconds(expiresSeconds);
            return Instant.now().isAfter(expireAt);
        } catch (Exception e) {
            log.debug("解析豆包临时URL过期时间失败: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, String> parseQueryParams(String url) {
        Map<String, String> queryParams = new HashMap<>();
        try {
            String query = URI.create(url).getRawQuery();
            if (StringUtils.isBlank(query)) {
                return queryParams;
            }
            for (String pair : query.split("&")) {
                if (StringUtils.isBlank(pair)) {
                    continue;
                }
                String[] parts = pair.split("=", 2);
                queryParams.put(parts[0], parts.length > 1 ? parts[1] : "");
            }
        } catch (Exception ignored) {
        }
        return queryParams;
    }

    private void syncPublishedPostMedia(UserVideoEntity video, String playableUrl) {
        if (video == null || video.getPostId() == null || StringUtils.isBlank(playableUrl)) {
            return;
        }
        PostEntity post = postService.getById(video.getPostId());
        if (post == null) {
            return;
        }
        List<String> currentMedia = JsonUtils.JsonToList(post.getMedia());
        String currentVideoUrl = currentMedia.isEmpty() ? "" : JsonUtils.normalizeUrlValue(currentMedia.get(0));
        if (StringUtils.equals(currentVideoUrl, playableUrl)) {
            return;
        }
        post.setMedia(JSON.toJSONString(Collections.singletonList(playableUrl)));
        postService.updateById(post);
    }
}
