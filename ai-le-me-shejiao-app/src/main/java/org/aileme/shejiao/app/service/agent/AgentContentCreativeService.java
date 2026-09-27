package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.api.service.UserVideoService;
import org.aileme.shejiao.api.service.VideoTemplateService;
import org.aileme.shejiao.app.oss.factory.RuoyiSysClound;
import org.aileme.shejiao.app.service.ai.image.AIImageStrategyManager;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.entity.app.UserVideoEntity;
import org.aileme.shejiao.domain.entity.app.VideoTemplateEntity;
import org.aileme.shejiao.domain.param.app.AddPostForm;
import org.aileme.shejiao.domain.param.app.AgentContentForm;
import org.aileme.shejiao.domain.param.app.GenerateVideoForm;
import org.aileme.shejiao.domain.vo.AgentContentDraftVo;
import org.aileme.shejiao.domain.vo.VideoGenerateVo;
import org.aileme.shejiao.gateway.chat.ChatModelGatewayService;
import org.aileme.shejiao.gateway.runtime.ThirdPartyRouteConfigService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Agent 内容创作服务
 */
@Slf4j
@Service
public class AgentContentCreativeService {

    private static final String TYPE_IMAGE_POST = "image_post";
    private static final String TYPE_DYNAMIC_IMAGE_POST = "dynamic_image_post";
    private static final String TYPE_AI_VIDEO = "ai_video";
    private static final String PLANNER_MODE = "three_branch_agent";
    private static final String DEFAULT_NEGATIVE =
            "低俗暴露、恐怖血腥、多人抢镜、脸部崩坏、手部畸形、塑料磨皮、廉价滤镜、奇怪肢体、夸张特效";
    private static final long GENERATED_IMAGE_UPLOAD_TIMEOUT_SECONDS = 45L;

    private static final List<CreativeMotif> CREATIVE_MOTIFS = List.of(
            CreativeMotif.of(
                    "hanfu_dynasty",
                    "朝代汉服定妆",
                    "costume_change",
                    List.of("汉服", "朝代", "唐风", "宋风", "明制", "马面裙", "换装", "古风"),
                    "古风电影定妆感，服饰层次精致，人物依旧是真人脸，温柔但不影楼。",
                    "庭院、长廊、园林或古建筑内景，季节感柔和，有轻风和留白。",
                    "半身到中景切换，衣摆和发丝有自然动势，眼神克制，镜头轻推进。",
                    "把现代真人照片延展成更高级的朝代汉服大片。",
                    "dynasty_hanfu_switch",
                    "汉服换装",
                    "elegant",
                    4
            ),
            CreativeMotif.of(
                    "anime_coser",
                    "动漫真人 Coser",
                    "cosplay",
                    List.of("动漫", "cos", "cose", "coser", "二次元", "漫改", "角色扮演"),
                    "保留真人五官一致性，发型和服装有二次元改编感，颜色鲜明但不廉价。",
                    "棚拍、舞台边、城市夜景或角色感室内空间，背景干净有聚光感。",
                    "先给人物一个正面定格，再用侧脸和细节特写强化角色感。",
                    "适合做带角色设定的人像大片或短片。",
                    "anime_coser_real",
                    "动漫 Coser",
                    "energetic",
                    4
            ),
            CreativeMotif.of(
                    "indoor_film_portrait",
                    "室内胶片妆造",
                    "cinematic_portrait",
                    List.of("胶片", "电影感", "妆造", "复古写真", "室内", "定妆", "港风"),
                    "35mm 胶片颗粒、暖色钨丝灯、真实肤质，像影视定妆照而不是写真楼风格。",
                    "窗边、化妆镜、复古房间、咖啡馆一角，空间细节温暖但不过满。",
                    "以半身和近景为主，手部动作自然，镜头从静帧过渡到轻微推拉。",
                    "适合做第一眼就很出片的电影感人物卡。",
                    "indoor_film_makeup",
                    "胶片妆造",
                    "elegant",
                    4
            ),
            CreativeMotif.of(
                    "snake_spirit",
                    "东方蛇妖觉醒",
                    "fantasy_transform",
                    List.of("蛇妖", "妖怪", "妖化", "异瞳", "东方奇幻", "蛇系"),
                    "墨绿与暗金配色，神秘高冷，不低俗不恐怖，保留人物识别度。",
                    "暗夜庭院、雾气水岸、宫殿回廊，画面克制，奇幻氛围来自光影而非廉价特效。",
                    "先稳住人物脸，再通过回眸、指尖和裙摆制造妖系张力。",
                    "适合做强设定的奇幻人物海报和短片。",
                    "snake_spirit_awakening",
                    "妖系变身",
                    "romantic",
                    4
            ),
            CreativeMotif.of(
                    "bone_spirit",
                    "白骨精前传",
                    "fantasy_transform",
                    List.of("白骨精", "白骨", "妖姬", "月夜", "骨相", "东方妖怪"),
                    "苍白骨相美人感，银白配饰，东方妖异审美，禁止猎奇和血腥。",
                    "冷雾庭院、月夜回廊、荒寺前院，背景简洁但氛围浓。",
                    "多用静帧凝视和侧脸停顿，镜头节奏偏慢，突出人物压迫感。",
                    "适合做冷艳系的高设定角色内容。",
                    "bone_spirit_moonlight",
                    "白骨精前传",
                    "elegant",
                    4
            ),
            CreativeMotif.of(
                    "growth_timeline",
                    "回到过去的成长线",
                    "growth_story",
                    List.of("成长", "回到过去", "童年", "从小到大", "时间线", "少女成长"),
                    "从童年记忆到成年高光的连续故事感，温暖、真诚，不做未成年性感化。",
                    "校园、旧街道、课桌、成人生活场景交替出现，像记忆册里的时间拼贴。",
                    "镜头按时间推进，从旧画面到当下，适合做 3-5 张故事卡或成长短片。",
                    "适合做有时间线和情绪起伏的内容。",
                    "childhood_to_beauty",
                    "成长故事",
                    "romantic",
                    5
            ),
            CreativeMotif.of(
                    "urban_magazine",
                    "都市杂志大片",
                    "daily",
                    List.of("都市", "杂志", "封面", "时尚", "高定", "大片", "通勤"),
                    "杂志封面式构图，利落穿搭，主体干净，都市感强但仍然真实自然。",
                    "写字楼、街角、咖啡馆、地铁口或高层玻璃窗边，现代感明确。",
                    "从正面主视觉开始，再切细节和侧身动作，节奏利落。",
                    "适合做职业设定、人设名片和都市感图文。",
                    "retro_movie_poster",
                    "都市大片",
                    "energetic",
                    4
            ),
            CreativeMotif.of(
                    "guofeng_healing",
                    "国风治愈系",
                    "daily",
                    List.of("治愈", "桃花", "煮茶", "国风", "古风", "花下", "仙气"),
                    "轻纱、晨雾、自然光和国风留白，整组内容温柔、松弛、治愈。",
                    "桃花树下、茶席旁、院子角落、溪边石桥，色调柔和通透。",
                    "镜头轻慢，人物动作尽量自然，适合做有呼吸感的图文故事卡。",
                    "适合婚恋社区里温柔高级的氛围内容。",
                    "hanfu_blossom_closeup",
                    "国风治愈",
                    "romantic",
                    4
            ),
            CreativeMotif.of(
                    "career_poster",
                    "职业设定海报",
                    "self_intro",
                    List.of("职业", "人设", "名片", "形象照", "海报", "宣传片"),
                    "突出职业身份和个人气场，但不过分商务化，仍然保留恋爱感和社交感。",
                    "围绕工作空间、城市生活场景和日常细节，让职业身份更可信。",
                    "先稳住一张人物主海报，再通过动作和场景补足生活化细节。",
                    "适合做主理人/职业女性/职业介绍类内容。",
                    "retro_movie_poster",
                    "职业人设",
                    "natural",
                    4
            ),
            CreativeMotif.of(
                    "romance_short",
                    "恋爱氛围短片",
                    "dating",
                    List.of("恋爱", "氛围", "约会", "喜欢", "想认识", "心动", "甜感"),
                    "柔和但不腻，暧昧感来自光线和眼神，不靠夸张滤镜。",
                    "街角夜灯、餐桌、书店、落地窗边、晚风阳台，空间有生活气息。",
                    "镜头轻推进，更多情绪近景和停顿，适合视频首眼吸引。",
                    "适合做轻恋爱感短片和图文封面。",
                    "self_intro_fresh",
                    "恋爱氛围",
                    "romantic",
                    4
            )
    );

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private UserVideoService userVideoService;

    @Autowired
    private VideoTemplateService videoTemplateService;

    @Autowired
    private PostService postService;

    @Autowired
    private AgentSafetyService safetyService;

    @Autowired(required = false)
    private ChatModelGatewayService chatModelGatewayService;

    @Autowired(required = false)
    private ThirdPartyRouteConfigService routeConfigService;

    @Autowired(required = false)
    private AIImageStrategyManager imageStrategyManager;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient imageDownloadClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public AgentContentDraftVo draft(AppUserEntity currentUser, AgentContentForm form) {
        AppUserEntity user = appUserService.getById(currentUser.getUid());
        String contentType = normalizeType(firstNonBlank(form.getContentType(), form.getRailType()));
        List<String> selectedReferenceMedia = collectSelectedReferenceMedia(form.getMedia(), maxReferenceCount(contentType));
        List<String> referenceMedia = collectProfileImages(user, selectedReferenceMedia, maxReferenceCount(contentType));
        ensureCreativeReferenceMedia(referenceMedia);
        CreativePlan plan = buildCreativePlan(user, form, contentType, referenceMedia);
        return toDraftVo(plan, referenceMedia, null, null, "planner_only", imageStrategyManager != null);
    }

    public AgentContentDraftVo create(AppUserEntity currentUser, AgentContentForm form) {
        AppUserEntity user = appUserService.getById(currentUser.getUid());
        String contentType = normalizeType(firstNonBlank(form.getContentType(), form.getRailType()));
        List<String> selectedReferenceMedia = collectSelectedReferenceMedia(form.getMedia(), maxReferenceCount(contentType));
        List<String> referenceMedia = collectProfileImages(user, selectedReferenceMedia, maxReferenceCount(contentType));
        ensureCreativeReferenceMedia(referenceMedia);

        CreativePlan plan = buildCreativePlan(user, form, contentType, referenceMedia);
        CreativeBranch selectedBranch = selectBranch(plan, form);
        CreativeBranch effectiveBranch = applyPromptDraft(selectedBranch, form.getPromptDraft(), contentType, referenceMedia);

        if (TYPE_AI_VIDEO.equals(contentType)) {
            return createVideoContent(user, form, referenceMedia, plan, effectiveBranch);
        }
        if (TYPE_DYNAMIC_IMAGE_POST.equals(contentType)) {
            return createDynamicImagePost(user, form, referenceMedia, plan, effectiveBranch);
        }
        return createImagePost(user, form, referenceMedia, plan, effectiveBranch);
    }

    private AgentContentDraftVo createImagePost(AppUserEntity user,
                                                AgentContentForm form,
                                                List<String> referenceMedia,
                                                CreativePlan plan,
                                                CreativeBranch branch) {
        GeneratedMediaResult generated = generateImagePostMedia(branch.getFinalPrompt(), referenceMedia, branch.getExecutionPreset());
        AddPostForm request = new AddPostForm();
        request.setType(1);
        request.setTopicId(form.getTopicId() == null ? 1 : form.getTopicId());
        request.setActivityId(form.getActivityId());
        request.setTitle(branch.getTitle());
        request.setContent(branch.getContent());
        request.setMedia(generated.getMedia());
        request.setCut(0);
        Integer postId = postService.addPost(request, user);
        return toDraftVo(plan, generated.getMedia(), postId, null, generated.getMode(), true, branch);
    }

    private AgentContentDraftVo createDynamicImagePost(AppUserEntity user,
                                                       AgentContentForm form,
                                                       List<String> referenceMedia,
                                                       CreativePlan plan,
                                                       CreativeBranch branch) {
        GeneratedMediaResult generated = generateImagePostMedia(branch.getFinalPrompt(), referenceMedia, branch.getExecutionPreset());
        AddPostForm request = new AddPostForm();
        request.setType(1);
        request.setTopicId(form.getTopicId() == null ? 1 : form.getTopicId());
        request.setActivityId(form.getActivityId());
        request.setTitle(branch.getTitle());
        request.setContent(branch.getContent());
        request.setMedia(generated.getMedia());
        request.setCut(0);
        Integer postId = postService.addPost(request, user);

        Integer videoId = null;
        try {
            videoId = createDynamicCoverVideo(user, branch, generated.getMedia());
        } catch (Exception ex) {
            log.warn("[agent-content] dynamic cover video skipped: {}", ex.getMessage());
        }

        return toDraftVo(plan, generated.getMedia(), postId, videoId, "image_story_motion", true, branch);
    }

    private AgentContentDraftVo createVideoContent(AppUserEntity user,
                                                   AgentContentForm form,
                                                   List<String> referenceMedia,
                                                   CreativePlan plan,
                                                   CreativeBranch branch) {
        VideoTemplateEntity template = resolveTemplateByBranch(branch, form);
        GenerateVideoForm request = new GenerateVideoForm();
        request.setTemplateCode(template.getCode());
        request.setCustomPrompt(branch.getFinalPrompt());
        request.setPostContent(branch.getContent());
        request.setAutoPublish(form.getAutoPublish());
        request.setMedia(referenceMedia);
        VideoGenerateVo generated = userVideoService.generateVideo(user.getUid(), request);
        return toDraftVo(plan, referenceMedia, null, generated.getVideoId(), "image_to_video", true, branch);
    }

    private Integer createDynamicCoverVideo(AppUserEntity user, CreativeBranch branch, List<String> generatedMedia) {
        if (generatedMedia == null || generatedMedia.isEmpty()) {
            return null;
        }
        VideoTemplateEntity template = resolveTemplateByCode(branch.getExecutionPreset().getTemplateCode());
        if (template == null) {
            AgentContentForm fallbackForm = new AgentContentForm();
            fallbackForm.setCategory(branch.getExecutionPreset().getCategory());
            fallbackForm.setGoal(branch.getIntentSummary());
            template = resolveTemplate(fallbackForm);
        }
        GenerateVideoForm request = new GenerateVideoForm();
        request.setTemplateCode(template.getCode());
        request.setCustomPrompt(buildDynamicCoverPrompt(branch));
        request.setPostContent(branch.getContent());
        request.setAutoPublish(0);
        request.setMedia(Collections.singletonList(generatedMedia.get(0)));
        VideoGenerateVo result = userVideoService.generateVideo(user.getUid(), request);
        return result == null ? null : result.getVideoId();
    }

    private String buildDynamicCoverPrompt(CreativeBranch branch) {
        return String.join("\n",
                branch.getFinalPrompt(),
                "输出要求：基于首张参考图制作 4-6 秒轻动态封面短片。",
                "动作要求：人物动作幅度很轻，更多是眼神、发丝、衣摆、环境光和镜头缓慢推进。",
                "画面要求：适合作为图文动态封面，不要夸张转场，不要强特效，不要多人。"
        );
    }

    private CreativePlan buildCreativePlan(AppUserEntity user,
                                           AgentContentForm form,
                                           String contentType,
                                           List<String> referenceMedia) {
        List<CreativeMotif> motifs = pickMotifs(user, form, contentType);
        List<CreativeBranch> branches = new ArrayList<>();
        for (int i = 0; i < motifs.size(); i++) {
            branches.add(buildCreativeBranch(user, form, contentType, motifs.get(i), i, referenceMedia));
        }
        branches = polishBranchesByLlm(user, form, contentType, branches);
        CreativeBranch selected = selectBranch(branches, form.getBranchId(), form.getMotifCode());
        if (selected == null && !branches.isEmpty()) {
            selected = branches.get(0);
        }
        if (selected == null) {
            throw new LinfengException("暂时无法生成可用的创意方案");
        }
        selected = applyPromptDraft(selected, form.getPromptDraft(), contentType, referenceMedia);
        List<CreativeBranch> normalized = new ArrayList<>();
        for (CreativeBranch branch : branches) {
            if (Objects.equals(branch.getBranchId(), selected.getBranchId())) {
                normalized.add(selected);
            } else {
                normalized.add(branch);
            }
        }
        String intentSummary = buildIntentSummary(user, form, contentType, selected, referenceMedia);
        return CreativePlan.builder()
                .contentType(contentType)
                .intentSummary(intentSummary)
                .selectedBranch(selected)
                .branches(normalized)
                .build();
    }

    private List<CreativeBranch> polishBranchesByLlm(AppUserEntity user,
                                                     AgentContentForm form,
                                                     String contentType,
                                                     List<CreativeBranch> branches) {
        if (chatModelGatewayService == null || branches == null || branches.isEmpty()) {
            return branches;
        }
        try {
            JsonNode node = requestJsonPlan(
                    "agent_content_planner",
                    "你是婚恋交友平台的真人照片内容共创策划 agent。给定 3 个创意方向，返回更像平台内容的标题和文案。必须只返回 JSON，不要 markdown。JSON 结构：{\"intentSummary\":\"\",\"branches\":[{\"branchId\":\"\",\"title\":\"\",\"content\":\"\"}]}。标题 8-18 字，文案 35-120 字。",
                    String.join("\n",
                            "创作轨道：" + getRailLabel(contentType),
                            "用户资料：" + buildProfileSummary(user),
                            "用户需求：" + StringUtils.defaultIfBlank(form.getGoal(), "帮我做一版更高级、更真实的人像内容"),
                            "分支列表：",
                            buildBranchPromptText(branches)
                    )
            );
            if (node == null || !node.isObject()) {
                return branches;
            }
            Map<String, JsonNode> branchNodeMap = new LinkedHashMap<>();
            JsonNode branchArray = node.path("branches");
            if (branchArray.isArray()) {
                for (JsonNode item : branchArray) {
                    String branchId = item.path("branchId").asText("");
                    if (StringUtils.isNotBlank(branchId)) {
                        branchNodeMap.put(branchId, item);
                    }
                }
            }
            List<CreativeBranch> result = new ArrayList<>();
            for (CreativeBranch branch : branches) {
                JsonNode branchNode = branchNodeMap.get(branch.getBranchId());
                if (branchNode == null) {
                    result.add(branch);
                    continue;
                }
                result.add(branch.toBuilder()
                        .title(limitText(branchNode.path("title").asText(branch.getTitle()), 24, branch.getTitle()))
                        .content(limitText(branchNode.path("content").asText(branch.getContent()), 380, branch.getContent()))
                        .build());
            }
            return result;
        } catch (Exception ex) {
            log.warn("[agent-content] polish plan by llm failed: {}", ex.getMessage());
            return branches;
        }
    }

    private String buildBranchPromptText(List<CreativeBranch> branches) {
        StringBuilder builder = new StringBuilder();
        for (CreativeBranch branch : branches) {
            builder.append(branch.getBranchId()).append(" | ")
                    .append(branch.getMotifName()).append(" | ")
                    .append(branch.getStyleDirection()).append(" | ")
                    .append(branch.getScenePlan()).append(" | ")
                    .append(branch.getShotPlan()).append('\n');
        }
        return builder.toString().trim();
    }

    private List<CreativeMotif> pickMotifs(AppUserEntity user, AgentContentForm form, String contentType) {
        String goal = StringUtils.lowerCase(StringUtils.defaultString(form.getGoal()));
        String recentCorpus = collectRecentCreativeCorpus(user);
        List<CreativeMotifScore> scored = new ArrayList<>();
        for (CreativeMotif motif : CREATIVE_MOTIFS) {
            int score = keywordScore(goal, motif.getKeywords()) * 120;
            if (StringUtils.equalsIgnoreCase(form.getCategory(), motif.getCategory())) {
                score += 80;
            }
            if (StringUtils.equalsIgnoreCase(form.getMotifCode(), motif.getCode())) {
                score += 260;
            }
            if (StringUtils.containsIgnoreCase(recentCorpus, motif.getCode())) {
                score -= 140;
            }
            score -= keywordScore(recentCorpus, motif.getKeywords()) * 30;
            if (TYPE_AI_VIDEO.equals(contentType) && StringUtils.isNotBlank(motif.getVideoTemplateCode())) {
                score += 30;
            }
            if (TYPE_DYNAMIC_IMAGE_POST.equals(contentType)) {
                score += motif.getDefaultImageCount() >= 4 ? 12 : 0;
            }
            score += Math.abs(Objects.hash(user.getUid(), defaultInt(form.getRefreshIndex()), motif.getCode())) % 17;
            scored.add(new CreativeMotifScore(motif, score));
        }
        scored.sort(Comparator.comparingInt(CreativeMotifScore::getScore).reversed());

        List<CreativeMotif> pool = new ArrayList<>();
        for (CreativeMotifScore score : scored) {
            pool.add(score.getMotif());
            if (pool.size() >= 6) {
                break;
            }
        }
        if (pool.isEmpty()) {
            pool.addAll(CREATIVE_MOTIFS.subList(0, Math.min(3, CREATIVE_MOTIFS.size())));
        }

        int offset = pool.isEmpty() ? 0 : Math.floorMod(defaultInt(form.getRefreshIndex()), pool.size());
        List<CreativeMotif> picked = new ArrayList<>();
        Set<String> usedCategories = new LinkedHashSet<>();
        for (int i = 0; i < pool.size() && picked.size() < 3; i++) {
            CreativeMotif motif = pool.get((offset + i) % pool.size());
            if (usedCategories.contains(motif.getCategory()) && pool.size() > 3) {
                continue;
            }
            picked.add(motif);
            usedCategories.add(motif.getCategory());
        }
        for (CreativeMotif motif : pool) {
            if (picked.size() >= 3) {
                break;
            }
            if (!picked.contains(motif)) {
                picked.add(motif);
            }
        }
        while (picked.size() < 3 && picked.size() < CREATIVE_MOTIFS.size()) {
            CreativeMotif motif = CREATIVE_MOTIFS.get(picked.size());
            if (!picked.contains(motif)) {
                picked.add(motif);
            }
        }
        return picked;
    }

    private String collectRecentCreativeCorpus(AppUserEntity user) {
        StringBuilder builder = new StringBuilder();
        try {
            List<UserVideoEntity> recentVideos = userVideoService.lambdaQuery()
                    .eq(UserVideoEntity::getUid, user.getUid())
                    .orderByDesc(UserVideoEntity::getId)
                    .last("limit 8")
                    .list();
            for (UserVideoEntity item : recentVideos) {
                builder.append(' ')
                        .append(StringUtils.defaultString(item.getTemplateCode()))
                        .append(' ')
                        .append(StringUtils.defaultString(item.getTitle()))
                        .append(' ')
                        .append(StringUtils.defaultString(item.getPromptText()));
            }
        } catch (Exception ex) {
            log.warn("[agent-content] load recent videos failed: {}", ex.getMessage());
        }
        try {
            List<PostEntity> recentPosts = postService.lambdaQuery()
                    .eq(PostEntity::getUid, user.getUid())
                    .orderByDesc(PostEntity::getId)
                    .last("limit 8")
                    .list();
            for (PostEntity item : recentPosts) {
                builder.append(' ')
                        .append(StringUtils.defaultString(item.getTitle()))
                        .append(' ')
                        .append(StringUtils.defaultString(item.getContent()));
            }
        } catch (Exception ex) {
            log.warn("[agent-content] load recent posts failed: {}", ex.getMessage());
        }
        return StringUtils.lowerCase(builder.toString());
    }

    private CreativeBranch buildCreativeBranch(AppUserEntity user,
                                               AgentContentForm form,
                                               String contentType,
                                               CreativeMotif motif,
                                               int index,
                                               List<String> referenceMedia) {
        PromptDraftData promptDraft = buildPromptDraft(user, form, contentType, motif, referenceMedia);
        ExecutionPresetData preset = buildExecutionPreset(contentType, motif);
        String finalPrompt = assembleFinalPrompt(contentType, promptDraft, motif, form.getGoal(), referenceMedia);
        String subjectProfile = buildSubjectProfile(user, referenceMedia);
        String title = buildBranchTitle(user, form, contentType, motif, index);
        String content = buildBranchContent(user, form, motif);
        String intentSummary = buildBranchIntentSummary(form.getGoal(), motif, contentType);
        return CreativeBranch.builder()
                .branchId("branch_" + (index + 1) + "_" + motif.getCode())
                .contentType(contentType)
                .motifCode(motif.getCode())
                .motifName(motif.getName())
                .title(title)
                .content(content)
                .intentSummary(intentSummary)
                .subjectProfile(subjectProfile)
                .styleDirection(motif.getStyleLine())
                .scenePlan(motif.getSceneLine())
                .shotPlan(motif.getShotLine())
                .negativePrompt(promptDraft.getNegativePrompt())
                .finalPrompt(finalPrompt)
                .promptDraft(promptDraft)
                .executionPreset(preset)
                .build();
    }

    private String buildIntentSummary(AppUserEntity user,
                                      AgentContentForm form,
                                      String contentType,
                                      CreativeBranch selected,
                                      List<String> referenceMedia) {
        String goal = StringUtils.defaultIfBlank(form.getGoal(), "想把真人照片做成更有辨识度的内容");
        String city = firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity(), "这座城市");
        return limitText("基于 " + referenceMedia.size() + " 张真人照片，为 " + city + " 的 " + getRailLabel(contentType)
                + "先策划 3 套方向，当前推荐「" + selected.getMotifName() + "」，目标是 " + goal + "。", 220, "已为你生成 3 套内容方向。");
    }

    private String buildBranchIntentSummary(String goal, CreativeMotif motif, String contentType) {
        String prefix = switch (contentType) {
            case TYPE_AI_VIDEO -> "先做视频意图策划";
            case TYPE_DYNAMIC_IMAGE_POST -> "先做多图故事卡与轻动态封面策划";
            default -> "先做连贯图文策划";
        };
        return limitText(prefix + "，围绕「" + motif.getName() + "」展开，" + StringUtils.defaultIfBlank(goal, motif.getGuideText()), 160,
                "围绕该方向做一版更有辨识度的真人照片内容。");
    }

    private String buildSubjectProfile(AppUserEntity user, List<String> referenceMedia) {
        String city = firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity(), "城市待补充");
        return limitText(String.join(" · ",
                        StringUtils.defaultIfBlank(user.getUsername(), "TA"),
                        getGenderText(user.getGender()),
                        valueOrDefault(user.getAge(), "年龄未知"),
                        city,
                        StringUtils.defaultIfBlank(user.getJob(), "职业待补充"),
                        "真人图 " + referenceMedia.size() + " 张"),
                120,
                "真人照片共创");
    }

    private PromptDraftData buildPromptDraft(AppUserEntity user,
                                             AgentContentForm form,
                                             String contentType,
                                             CreativeMotif motif,
                                             List<String> referenceMedia) {
        String city = firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity(), "这座城市");
        String goal = StringUtils.defaultIfBlank(form.getGoal(), motif.getGuideText());
        String subject = limitText(
                StringUtils.defaultIfBlank(user.getUsername(), "TA") + "，"
                        + getGenderText(user.getGender()) + "，"
                        + valueOrDefault(user.getAge(), "年龄未知") + "岁，常住" + city + "，职业"
                        + StringUtils.defaultIfBlank(user.getJob(), "待补充")
                        + "，兴趣" + StringUtils.defaultIfBlank(user.getInterest(), "散步、阅读、旅行")
                        + "，参考 " + referenceMedia.size() + " 张真人头像/生活照，人物五官和年龄感必须保持一致。",
                220,
                "参考真人照片，保留人物脸部识别度和真实年龄感。"
        );
        String style = limitText(motif.getStyleLine() + " 创作目标：" + goal, 220, motif.getStyleLine());
        String scene = limitText(motif.getSceneLine(), 180, "围绕真实生活场景展开。");
        String shot = limitText(motif.getShotLine() + " " + getRailShotGuide(contentType), 180, motif.getShotLine());
        String negative = limitText(DEFAULT_NEGATIVE, 160, DEFAULT_NEGATIVE);
        return PromptDraftData.builder()
                .subject(subject)
                .style(style)
                .scene(scene)
                .shot(shot)
                .negativePrompt(negative)
                .build();
    }

    private String getRailShotGuide(String contentType) {
        if (TYPE_AI_VIDEO.equals(contentType)) {
            return "9:16 竖屏，镜头节奏清晰，适合生成 5-15 秒真人质感短片。";
        }
        if (TYPE_DYNAMIC_IMAGE_POST.equals(contentType)) {
            return "优先适配 3-5 张故事卡，首图需要有轻动态封面延展空间。";
        }
        return "适配 3-5 张图文卡，首图要抓眼，后续画面要连贯。";
    }

    private ExecutionPresetData buildExecutionPreset(String contentType, CreativeMotif motif) {
        String mode = TYPE_AI_VIDEO.equals(contentType) ? "image_to_video"
                : TYPE_DYNAMIC_IMAGE_POST.equals(contentType) ? "image_story_motion"
                : "image_story";
        return ExecutionPresetData.builder()
                .mode(mode)
                .templateCode(motif.getVideoTemplateCode())
                .templateName(motif.getVideoTemplateName())
                .category(motif.getCategory())
                .stylePreset(motif.getStylePreset())
                .imageCount(TYPE_AI_VIDEO.equals(contentType) ? 4 : motif.getDefaultImageCount())
                .estimatedCost(TYPE_AI_VIDEO.equals(contentType) ? 20 : TYPE_DYNAMIC_IMAGE_POST.equals(contentType) ? 15 : 10)
                .dynamicCover(TYPE_DYNAMIC_IMAGE_POST.equals(contentType))
                .build();
    }

    private String assembleFinalPrompt(String contentType,
                                       PromptDraftData promptDraft,
                                       CreativeMotif motif,
                                       String goal,
                                       List<String> referenceMedia) {
        StringBuilder builder = new StringBuilder();
        builder.append("为婚恋交友平台生成 ").append(getRailLabel(contentType)).append(" 内容。").append('\n');
        builder.append("创意母题：").append(motif.getName()).append("。").append('\n');
        builder.append("人物设定：").append(promptDraft.getSubject()).append('\n');
        builder.append("风格氛围：").append(promptDraft.getStyle()).append('\n');
        builder.append("场景规划：").append(promptDraft.getScene()).append('\n');
        builder.append("镜头动作：").append(promptDraft.getShot()).append('\n');
        builder.append("素材要求：参考 ").append(referenceMedia.size()).append(" 张真人照片，保持同一人设，不得换脸。").append('\n');
        if (StringUtils.isNotBlank(goal)) {
            builder.append("用户目标：").append(limitText(goal, 120, goal)).append('\n');
        }
        builder.append("输出要求：").append(getRailOutputGuide(contentType, motif)).append('\n');
        builder.append("限制词：").append(promptDraft.getNegativePrompt());
        return limitText(builder.toString(), 1200, motif.getGuideText());
    }

    private String getRailOutputGuide(String contentType, CreativeMotif motif) {
        if (TYPE_AI_VIDEO.equals(contentType)) {
            return "9:16 竖屏，真人写实，镜头有轻微推进和停顿，适合社交平台短视频发布。";
        }
        if (TYPE_DYNAMIC_IMAGE_POST.equals(contentType)) {
            return "输出 3-5 张连贯图文卡，首图具备轻动态封面延展空间，故事感明确。";
        }
        return "输出 3-5 张连贯图文卡，首图抓眼，后续画面衔接自然，适合发真人氛围图文。";
    }

    private String buildBranchTitle(AppUserEntity user,
                                    AgentContentForm form,
                                    String contentType,
                                    CreativeMotif motif,
                                    int index) {
        String goal = StringUtils.defaultString(form.getGoal());
        if (StringUtils.isNotBlank(goal)) {
            return limitText(motif.getName() + " · " + goal, 24, motif.getName());
        }
        String suffix = switch (contentType) {
            case TYPE_AI_VIDEO -> "短片";
            case TYPE_DYNAMIC_IMAGE_POST -> "故事卡";
            default -> "图文";
        };
        return limitText(motif.getName() + suffix + (index == 0 ? "" : ""), 24, motif.getName());
    }

    private String buildBranchContent(AppUserEntity user, AgentContentForm form, CreativeMotif motif) {
        String city = firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity(), "这座城市");
        String interest = StringUtils.defaultIfBlank(user.getInterest(), "散步、阅读、旅行");
        String goal = StringUtils.defaultIfBlank(form.getGoal(), "想做一版更像自己的真人照片内容");
        return limitText("把真实头像和生活照往「" + motif.getName() + "」这个方向延展，"
                + "保留我在 " + city + " 的生活气息和日常状态。"
                + "平时喜欢 " + interest + "，这次更想表达：" + goal, 380, "基于真人照片生成一版更有辨识度的内容。");
    }

    private CreativeBranch selectBranch(CreativePlan plan, AgentContentForm form) {
        return selectBranch(plan.getBranches(), form.getBranchId(), form.getMotifCode());
    }

    private CreativeBranch selectBranch(List<CreativeBranch> branches, String branchId, String motifCode) {
        if (branches == null || branches.isEmpty()) {
            return null;
        }
        if (StringUtils.isNotBlank(branchId)) {
            for (CreativeBranch branch : branches) {
                if (StringUtils.equals(branchId, branch.getBranchId())) {
                    return branch;
                }
            }
        }
        if (StringUtils.isNotBlank(motifCode)) {
            for (CreativeBranch branch : branches) {
                if (StringUtils.equalsIgnoreCase(motifCode, branch.getMotifCode())) {
                    return branch;
                }
            }
        }
        return branches.get(0);
    }

    private CreativeBranch applyPromptDraft(CreativeBranch branch,
                                            AgentContentForm.PromptDraft promptDraft,
                                            String contentType,
                                            List<String> referenceMedia) {
        if (branch == null || promptDraft == null) {
            return branch;
        }
        PromptDraftData merged = branch.getPromptDraft().toBuilder()
                .subject(firstNonBlank(promptDraft.getSubject(), branch.getPromptDraft().getSubject()))
                .style(firstNonBlank(promptDraft.getStyle(), branch.getPromptDraft().getStyle()))
                .scene(firstNonBlank(promptDraft.getScene(), branch.getPromptDraft().getScene()))
                .shot(firstNonBlank(promptDraft.getShot(), branch.getPromptDraft().getShot()))
                .negativePrompt(firstNonBlank(promptDraft.getNegativePrompt(), branch.getPromptDraft().getNegativePrompt()))
                .build();
        String finalPrompt = assembleFinalPrompt(contentType, merged, resolveMotif(branch.getMotifCode()), branch.getIntentSummary(), referenceMedia);
        return branch.toBuilder()
                .subjectProfile(limitText(merged.getSubject(), 180, branch.getSubjectProfile()))
                .styleDirection(limitText(merged.getStyle(), 180, branch.getStyleDirection()))
                .scenePlan(limitText(merged.getScene(), 180, branch.getScenePlan()))
                .shotPlan(limitText(merged.getShot(), 180, branch.getShotPlan()))
                .negativePrompt(limitText(merged.getNegativePrompt(), 180, branch.getNegativePrompt()))
                .promptDraft(merged)
                .finalPrompt(finalPrompt)
                .build();
    }

    private CreativeMotif resolveMotif(String motifCode) {
        for (CreativeMotif motif : CREATIVE_MOTIFS) {
            if (StringUtils.equalsIgnoreCase(motifCode, motif.getCode())) {
                return motif;
            }
        }
        return CREATIVE_MOTIFS.get(0);
    }

    private AgentContentDraftVo toDraftVo(CreativePlan plan,
                                          List<String> media,
                                          Integer postId,
                                          Integer videoId,
                                          String imageMode,
                                          boolean imageSupported) {
        return toDraftVo(plan, media, postId, videoId, imageMode, imageSupported, plan.getSelectedBranch());
    }

    private AgentContentDraftVo toDraftVo(CreativePlan plan,
                                          List<String> media,
                                          Integer postId,
                                          Integer videoId,
                                          String imageMode,
                                          boolean imageSupported,
                                          CreativeBranch selectedBranch) {
        CreativeBranch branch = selectedBranch == null ? plan.getSelectedBranch() : selectedBranch;
        List<AgentContentDraftVo.BranchVo> branchList = new ArrayList<>();
        for (CreativeBranch item : plan.getBranches()) {
            branchList.add(toBranchVo(item));
        }
        return AgentContentDraftVo.builder()
                .contentType(plan.getContentType())
                .provider(currentProvider())
                .agentApplied(true)
                .title(branch.getTitle())
                .content(branch.getContent())
                .intentSummary(plan.getIntentSummary())
                .plannerMode(PLANNER_MODE)
                .media(media)
                .imageGenerationMode(imageMode)
                .imageGenerationSupported(imageSupported)
                .templateCode(branch.getExecutionPreset().getTemplateCode())
                .templateName(branch.getExecutionPreset().getTemplateName())
                .category(branch.getExecutionPreset().getCategory())
                .prompt(branch.getFinalPrompt())
                .selectedBranchId(branch.getBranchId())
                .motifCode(branch.getMotifCode())
                .motifName(branch.getMotifName())
                .subjectProfile(branch.getSubjectProfile())
                .styleDirection(branch.getStyleDirection())
                .scenePlan(branch.getScenePlan())
                .shotPlan(branch.getShotPlan())
                .negativePrompt(branch.getNegativePrompt())
                .finalPrompt(branch.getFinalPrompt())
                .editablePromptDraft(toPromptDraftVo(branch.getPromptDraft()))
                .executionPreset(toExecutionPresetVo(branch.getExecutionPreset()))
                .branchList(branchList)
                .postId(postId)
                .videoId(videoId)
                .build();
    }

    private AgentContentDraftVo.BranchVo toBranchVo(CreativeBranch branch) {
        return AgentContentDraftVo.BranchVo.builder()
                .branchId(branch.getBranchId())
                .contentType(branch.getContentType())
                .motifCode(branch.getMotifCode())
                .motifName(branch.getMotifName())
                .title(branch.getTitle())
                .content(branch.getContent())
                .intentSummary(branch.getIntentSummary())
                .subjectProfile(branch.getSubjectProfile())
                .styleDirection(branch.getStyleDirection())
                .scenePlan(branch.getScenePlan())
                .shotPlan(branch.getShotPlan())
                .negativePrompt(branch.getNegativePrompt())
                .finalPrompt(branch.getFinalPrompt())
                .editablePromptDraft(toPromptDraftVo(branch.getPromptDraft()))
                .executionPreset(toExecutionPresetVo(branch.getExecutionPreset()))
                .build();
    }

    private AgentContentDraftVo.PromptDraftVo toPromptDraftVo(PromptDraftData draft) {
        return AgentContentDraftVo.PromptDraftVo.builder()
                .subject(draft.getSubject())
                .style(draft.getStyle())
                .scene(draft.getScene())
                .shot(draft.getShot())
                .negativePrompt(draft.getNegativePrompt())
                .build();
    }

    private AgentContentDraftVo.ExecutionPresetVo toExecutionPresetVo(ExecutionPresetData preset) {
        return AgentContentDraftVo.ExecutionPresetVo.builder()
                .mode(preset.getMode())
                .templateCode(preset.getTemplateCode())
                .templateName(preset.getTemplateName())
                .category(preset.getCategory())
                .stylePreset(preset.getStylePreset())
                .imageCount(preset.getImageCount())
                .estimatedCost(preset.getEstimatedCost())
                .dynamicCover(preset.getDynamicCover())
                .build();
    }

    private GeneratedMediaResult generateImagePostMedia(String prompt,
                                                        List<String> referenceMedia,
                                                        ExecutionPresetData preset) {
        if (imageStrategyManager == null) {
            throw new LinfengException("AI 图片服务暂未配置");
        }
        ensureCreativeReferenceMedia(referenceMedia);
        try {
            List<String> generatedUrls = imageStrategyManager.generateImages(
                    prompt,
                    referenceMedia,
                    buildImageGenerateParams(referenceMedia, preset)
            );
            List<String> persisted = persistGeneratedImages(generatedUrls);
            if (!persisted.isEmpty()) {
                return GeneratedMediaResult.of(persisted, preset.getDynamicCover() ? "image_story_motion" : "image_story");
            }
        } catch (Exception ex) {
            log.warn("[agent-content] image generation failed: {}", ex.getMessage());
            throw new LinfengException("AI 图片生成失败，请稍后重试");
        }
        throw new LinfengException("AI 图片生成失败，请稍后重试");
    }

    private void ensureCreativeReferenceMedia(List<String> referenceMedia) {
        if (referenceMedia == null || referenceMedia.isEmpty()) {
            throw new LinfengException("请先上传真人头像或生活照，并至少选择 1 张真人图后再生成");
        }
    }

    private Map<String, Object> buildImageGenerateParams(List<String> referenceMedia, ExecutionPresetData preset) {
        Map<String, Object> params = new HashMap<>();
        params.put("function_type", Boolean.TRUE.equals(preset.getDynamicCover()) ? "agent_content_ai_dynamic" : "agent_content_ai_image");
        params.put("scene_code", "agent_content_" + preset.getMode());
        params.put("size", "2K");
        params.put("response_format", "url");
        params.put("watermark", true);
        params.put("max_images", preset.getImageCount());
        params.put("template_code", preset.getTemplateCode());
        params.put("sequential_image_generation", referenceMedia == null || referenceMedia.isEmpty() ? "disabled" : "auto");
        return params;
    }

    private List<String> persistGeneratedImages(List<String> generatedUrls) {
        if (generatedUrls == null || generatedUrls.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> persisted = new ArrayList<>();
        for (String url : generatedUrls) {
            String saved = persistGeneratedImage(url);
            if (StringUtils.isNotBlank(saved)) {
                persisted.add(saved);
            }
        }
        return persisted;
    }

    private String persistGeneratedImage(String imageUrl) {
        String normalizedUrl = StringUtils.trimToEmpty(imageUrl);
        if (StringUtils.isBlank(normalizedUrl)) {
            return "";
        }
        log.info("[agent-content] keep generated image source url directly: {}", normalizedUrl);
        return normalizedUrl;
    }

    private JsonNode requestJsonPlan(String scene, String systemPrompt, String userPrompt) {
        if (chatModelGatewayService == null) {
            return null;
        }
        try {
            String raw = chatModelGatewayService.complete(scene, systemPrompt, userPrompt, 800, 0.55D);
            if (StringUtils.isBlank(raw)) {
                return null;
            }
            return objectMapper.readTree(stripCodeFence(raw.trim()));
        } catch (Exception ex) {
            log.warn("[agent-content] llm plan failed, scene={}, reason={}", scene, ex.getMessage());
            return null;
        }
    }

    private VideoTemplateEntity resolveTemplateByBranch(CreativeBranch branch, AgentContentForm form) {
        VideoTemplateEntity byCode = resolveTemplateByCode(branch.getExecutionPreset().getTemplateCode());
        if (byCode != null) {
            return byCode;
        }
        AgentContentForm fallback = new AgentContentForm();
        fallback.setCategory(branch.getExecutionPreset().getCategory());
        fallback.setGoal(form.getGoal());
        fallback.setTemplateCode(form.getTemplateCode());
        return resolveTemplate(fallback);
    }

    private VideoTemplateEntity resolveTemplateByCode(String templateCode) {
        if (StringUtils.isBlank(templateCode)) {
            return null;
        }
        try {
            return videoTemplateService.getByCode(templateCode);
        } catch (Exception ex) {
            log.warn("[agent-content] resolve template by code failed: {}", ex.getMessage());
            return null;
        }
    }

    private VideoTemplateEntity resolveTemplate(AgentContentForm form) {
        if (StringUtils.isNotBlank(form.getTemplateCode())) {
            VideoTemplateEntity fixed = videoTemplateService.getByCode(form.getTemplateCode());
            if (fixed != null) {
                return fixed;
            }
        }
        String category = StringUtils.defaultIfBlank(form.getCategory(), guessCategoryByGoal(form.getGoal()));
        List<VideoTemplateEntity> candidates = videoTemplateService.lambdaQuery()
                .eq(VideoTemplateEntity::getStatus, VideoTemplateEntity.STATUS_ENABLED)
                .eq(StringUtils.isNotBlank(category), VideoTemplateEntity::getCategory, category)
                .orderByAsc(VideoTemplateEntity::getSort, VideoTemplateEntity::getId)
                .list();
        VideoTemplateEntity template = selectBestTemplate(candidates, form.getGoal());
        if (template != null) {
            return template;
        }
        List<VideoTemplateEntity> fallbackCandidates = videoTemplateService.lambdaQuery()
                .eq(VideoTemplateEntity::getStatus, VideoTemplateEntity.STATUS_ENABLED)
                .orderByAsc(VideoTemplateEntity::getSort, VideoTemplateEntity::getId)
                .list();
        VideoTemplateEntity fallback = selectBestTemplate(fallbackCandidates, form.getGoal());
        if (fallback != null) {
            return fallback;
        }
        VideoTemplateEntity localFallback = new VideoTemplateEntity();
        localFallback.setCode("self_intro_fresh");
        localFallback.setName("清新自我介绍");
        localFallback.setCategory("self_intro");
        localFallback.setPromptTemplate("围绕{username}在{city}的真实生活展开，展示{job}身份、{interest}爱好与{loveDeclaration}。");
        localFallback.setStylePreset("natural");
        localFallback.setMaxImages(4);
        localFallback.setDurationRange("8-12");
        return localFallback;
    }

    private List<String> collectProfileImages(AppUserEntity user, List<String> preferred, int maxCount) {
        Set<String> urls = new LinkedHashSet<>();
        if (preferred != null) {
            for (String item : preferred) {
                if (isUsableProfileImage(item)) {
                    urls.add(item.trim());
                }
                if (urls.size() >= maxCount) {
                    return new ArrayList<>(urls);
                }
            }
        }
        if (isUsableProfileImage(user.getAvatar())) {
            urls.add(user.getAvatar().trim());
        }
        if (StringUtils.isNotBlank(user.getFigur())) {
            try {
                for (String url : parseLegacyImageList(user.getFigur())) {
                    if (isUsableProfileImage(url)) {
                        urls.add(url.trim());
                    }
                    if (urls.size() >= maxCount) {
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warn("[agent-content] parse figur failed: {}", ex.getMessage());
            }
        }
        return new ArrayList<>(urls);
    }

    private List<String> collectSelectedReferenceMedia(List<String> selected, int maxCount) {
        if (selected == null || selected.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> urls = new LinkedHashSet<>();
        for (String item : selected) {
            if (isUsableProfileImage(item)) {
                urls.add(item.trim());
            }
            if (urls.size() >= maxCount) {
                break;
            }
        }
        return new ArrayList<>(urls);
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

    private List<String> parseLegacyImageList(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        String source = raw.trim();
        if (source.startsWith("[")) {
            List<String> jsonList = JSON.parseArray(source, String.class);
            return jsonList == null ? Collections.emptyList() : jsonList;
        }
        String[] split = source.split("\\s*,\\s*");
        List<String> urls = new ArrayList<>(split.length);
        for (String item : split) {
            if (StringUtils.isNotBlank(item)) {
                urls.add(item.trim());
            }
        }
        return urls;
    }

    private String buildProfileSummary(AppUserEntity user) {
        return String.join("，",
                StringUtils.defaultIfBlank(user.getUsername(), "TA"),
                StringUtils.defaultIfBlank(getGenderText(user.getGender()), "未知性别"),
                StringUtils.defaultIfBlank(valueOrDefault(user.getAge(), "年龄未知"), "年龄未知"),
                StringUtils.defaultIfBlank(firstNonBlank(user.getAbodeCity(), user.getLocationCity(), user.getCity()), "城市未知"),
                StringUtils.defaultIfBlank(user.getJob(), "职业待补充"),
                StringUtils.defaultIfBlank(getEducationText(user.getEducation()), "学历待补充"),
                "兴趣：" + StringUtils.defaultIfBlank(user.getInterest(), "生活记录"),
                "标签：" + StringUtils.defaultIfBlank(user.getTagStr(), "真诚"),
                "爱情观：" + StringUtils.defaultIfBlank(user.getLoveDeclaration(), "认真相处")
        );
    }

    private String getRailLabel(String contentType) {
        if (TYPE_AI_VIDEO.equals(contentType)) {
            return "AI 视频";
        }
        if (TYPE_DYNAMIC_IMAGE_POST.equals(contentType)) {
            return "AI 动态图文";
        }
        return "AI 图文";
    }

    private String guessCategoryByGoal(String goal) {
        String normalized = StringUtils.lowerCase(StringUtils.defaultString(goal));
        if (containsAny(normalized, "汉服", "朝代", "唐风", "宋风", "明制", "马面裙", "换装")) {
            return "costume_change";
        }
        if (containsAny(normalized, "cos", "cose", "coser", "动漫", "二次元", "漫改", "角色扮演")) {
            return "cosplay";
        }
        if (containsAny(normalized, "胶片", "妆造", "电影脸", "定妆", "复古写真", "室内人像", "港风")) {
            return "cinematic_portrait";
        }
        if (containsAny(normalized, "蛇妖", "白骨精", "妖怪", "妖化", "异瞳", "女妖")) {
            return "fantasy_transform";
        }
        if (containsAny(normalized, "童年", "成长", "回到过去", "从小到大", "时间线")) {
            return "growth_story";
        }
        if (containsAny(normalized, "约会", "恋爱", "想认识", "心动", "暧昧")) {
            return "dating";
        }
        return "self_intro";
    }

    private String normalizeType(String type) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(type));
        if (TYPE_AI_VIDEO.equals(normalized) || "video".equals(normalized)) {
            return TYPE_AI_VIDEO;
        }
        if (TYPE_DYNAMIC_IMAGE_POST.equals(normalized) || "dynamic".equals(normalized) || "dynamic_post".equals(normalized)) {
            return TYPE_DYNAMIC_IMAGE_POST;
        }
        return TYPE_IMAGE_POST;
    }

    private int maxReferenceCount(String contentType) {
        return TYPE_AI_VIDEO.equals(contentType) ? 4 : 6;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String currentProvider() {
        try {
            if (chatModelGatewayService != null) {
                String routed = chatModelGatewayService.currentProviderCode();
                if (StringUtils.isNotBlank(routed)) {
                    return routed;
                }
            }
            if (routeConfigService != null) {
                String routed = routeConfigService.getCurrentProviderCode("ai", "openai_codex");
                if (StringUtils.isNotBlank(routed)) {
                    return routed;
                }
            }
        } catch (Exception ex) {
            log.warn("[agent-content] read provider failed: {}", ex.getMessage());
        }
        return "openai_codex";
    }

    private String stripCodeFence(String text) {
        if (StringUtils.startsWith(text, "```") && StringUtils.endsWith(text, "```")) {
            String stripped = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            stripped = stripped.replaceFirst("\\s*```$", "");
            return stripped.trim();
        }
        return text;
    }

    private String limitText(String text, int max, String fallback) {
        String safe = safetyService.sanitizeText(StringUtils.defaultIfBlank(text, fallback), max);
        if (StringUtils.isBlank(safe)) {
            return fallback;
        }
        return safe.length() > max ? safe.substring(0, max) : safe;
    }

    private String getGenderText(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        return "未知";
    }

    private String getEducationText(Integer education) {
        if (education == null) {
            return "本科";
        }
        switch (education) {
            case 1:
                return "高中";
            case 2:
                return "大专";
            case 3:
                return "本科";
            case 4:
                return "硕士";
            case 5:
                return "博士";
            default:
                return "本科";
        }
    }

    private String valueOrDefault(Integer value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private VideoTemplateEntity selectBestTemplate(List<VideoTemplateEntity> templates, String goal) {
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        String normalizedGoal = StringUtils.lowerCase(StringUtils.defaultString(goal));
        VideoTemplateEntity best = null;
        int bestScore = Integer.MIN_VALUE;
        for (VideoTemplateEntity template : templates) {
            int score = scoreVideoTemplate(template, normalizedGoal);
            if (best == null || score > bestScore) {
                best = template;
                bestScore = score;
            }
        }
        return best;
    }

    private int scoreVideoTemplate(VideoTemplateEntity template, String normalizedGoal) {
        int score = 0;
        if (template == null) {
            return score;
        }
        score -= template.getSort() == null ? 0 : template.getSort();
        if (StringUtils.isBlank(normalizedGoal)) {
            return score;
        }
        String code = StringUtils.defaultString(template.getCode());
        score += keywordScore(normalizedGoal, keywordsForVideoTemplate(code)) * 100;
        String text = StringUtils.lowerCase(String.join(" ",
                StringUtils.defaultString(template.getName()),
                StringUtils.defaultString(template.getDescription()),
                StringUtils.defaultString(template.getCategory()),
                code
        ));
        if (StringUtils.isNotBlank(text)) {
            for (String keyword : keywordsForVideoTemplate(code)) {
                if (normalizedGoal.contains(keyword) && text.contains(keyword)) {
                    score += 12;
                }
            }
        }
        return score;
    }

    private List<String> keywordsForVideoTemplate(String code) {
        if ("dynasty_hanfu_switch".equalsIgnoreCase(code)) {
            return List.of("汉服", "朝代", "唐风", "宋风", "明制", "换装", "马面裙");
        }
        if ("hanfu_blossom_closeup".equalsIgnoreCase(code)) {
            return List.of("汉服", "桃花", "古风", "国风", "花下", "定妆");
        }
        if ("anime_coser_real".equalsIgnoreCase(code)) {
            return List.of("cos", "cose", "coser", "动漫", "二次元", "漫改");
        }
        if ("comic_heroine_stage".equalsIgnoreCase(code)) {
            return List.of("动漫", "角色", "舞台", "英雄", "少女", "漫改");
        }
        if ("indoor_film_makeup".equalsIgnoreCase(code)) {
            return List.of("胶片", "室内", "妆造", "电影", "定妆", "复古写真");
        }
        if ("retro_movie_poster".equalsIgnoreCase(code)) {
            return List.of("复古", "电影海报", "港风", "影楼", "定妆", "杂志", "海报");
        }
        if ("snake_spirit_awakening".equalsIgnoreCase(code)) {
            return List.of("蛇妖", "妖怪", "妖化", "异瞳", "女妖", "东方奇幻");
        }
        if ("bone_spirit_moonlight".equalsIgnoreCase(code)) {
            return List.of("白骨精", "白骨", "妖姬", "月夜", "骨相");
        }
        if ("childhood_to_beauty".equalsIgnoreCase(code)) {
            return List.of("童年", "成长", "回到过去", "从小到大", "时间线");
        }
        if ("pixel_youth_timeline".equalsIgnoreCase(code)) {
            return List.of("像素", "青春", "怀旧", "90年代", "成长");
        }
        return Collections.emptyList();
    }

    private int keywordScore(String text, List<String> keywords) {
        int score = 0;
        if (StringUtils.isBlank(text) || keywords == null) {
            return score;
        }
        for (String keyword : keywords) {
            if (StringUtils.isNotBlank(keyword) && text.contains(StringUtils.lowerCase(keyword))) {
                score++;
            }
        }
        return score;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StringUtils.isBlank(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.isNotBlank(keyword) && text.contains(StringUtils.lowerCase(keyword))) {
                return true;
            }
        }
        return false;
    }

    @lombok.Data
    @lombok.Builder
    private static class CreativePlan {
        private String contentType;
        private String intentSummary;
        private List<CreativeBranch> branches;
        private CreativeBranch selectedBranch;
    }

    @lombok.Data
    @lombok.Builder(toBuilder = true)
    private static class CreativeBranch {
        private String branchId;
        private String contentType;
        private String motifCode;
        private String motifName;
        private String title;
        private String content;
        private String intentSummary;
        private String subjectProfile;
        private String styleDirection;
        private String scenePlan;
        private String shotPlan;
        private String negativePrompt;
        private String finalPrompt;
        private PromptDraftData promptDraft;
        private ExecutionPresetData executionPreset;
    }

    @lombok.Data
    @lombok.Builder(toBuilder = true)
    private static class PromptDraftData {
        private String subject;
        private String style;
        private String scene;
        private String shot;
        private String negativePrompt;
    }

    @lombok.Data
    @lombok.Builder
    private static class ExecutionPresetData {
        private String mode;
        private String templateCode;
        private String templateName;
        private String category;
        private String stylePreset;
        private Integer imageCount;
        private Integer estimatedCost;
        private Boolean dynamicCover;
    }

    @lombok.Data
    @lombok.Builder
    private static class GeneratedMediaResult {
        private List<String> media;
        private String mode;

        private static GeneratedMediaResult of(List<String> media, String mode) {
            return GeneratedMediaResult.builder()
                    .media(media)
                    .mode(mode)
                    .build();
        }
    }

    @lombok.Getter
    @lombok.AllArgsConstructor(staticName = "of")
    private static class CreativeMotif {
        private final String code;
        private final String name;
        private final String category;
        private final List<String> keywords;
        private final String styleLine;
        private final String sceneLine;
        private final String shotLine;
        private final String guideText;
        private final String videoTemplateCode;
        private final String videoTemplateName;
        private final String stylePreset;
        private final Integer defaultImageCount;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class CreativeMotifScore {
        private final CreativeMotif motif;
        private final int score;
    }
}
