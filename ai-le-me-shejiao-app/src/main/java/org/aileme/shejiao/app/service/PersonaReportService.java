package org.aileme.shejiao.app.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.app.service.agent.AgentRuntimeBridgeService;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.UserPersonaSnapshotEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.vo.PersonaReportVO;
import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 画像报告生成服务
 */
@Service
public class PersonaReportService {

    private static final Pattern SINGLE_PATTERN = Pattern.compile("(?:单身|空窗)(?:约|近)?\\s*(\\d{1,2})(年|个月|月|天)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private TagProfileAggregateService tagProfileAggregateService;

    @Autowired(required = false)
    private AgentRuntimeBridgeService agentRuntimeBridgeService;

    public PersonaReportVO buildReport(Integer userId) {
        PersonaReportVO runtimeReport = buildRuntimeReport(userId);
        if (runtimeReport != null) {
            return runtimeReport;
        }

        AppUserEntity user = appUserService.getById(userId);
        if (user == null) {
            throw new LinfengException("用户不存在");
        }

        String uidStr = String.valueOf(userId);
        List<ChatMessageEntity> recentMessages = chatMessageService.lambdaQuery()
                .and(wrapper -> wrapper.eq(ChatMessageEntity::getSenderId, uidStr)
                        .or()
                        .eq(ChatMessageEntity::getReceiverId, uidStr))
                .orderByDesc(ChatMessageEntity::getId)
                .last("LIMIT 2000")
                .list();

        int sent = 0;
        int received = 0;
        Set<String> chattedUsers = new HashSet<>();
        Set<LocalDate> activeDays = new HashSet<>();
        List<String> textMessages = new ArrayList<>();

        for (ChatMessageEntity message : recentMessages) {
            if (message == null || Objects.equals(message.getIsWithdrawn(), 1)) {
                continue;
            }
            boolean selfSent = uidStr.equals(StringUtils.trimToEmpty(message.getSenderId()));
            if (selfSent) {
                sent++;
                if (StringUtils.isNotBlank(message.getReceiverId())) {
                    chattedUsers.add(message.getReceiverId());
                }
            } else {
                received++;
                if (StringUtils.isNotBlank(message.getSenderId())) {
                    chattedUsers.add(message.getSenderId());
                }
            }
            LocalDate day = resolveDay(message.getSendTime());
            if (day != null) {
                activeDays.add(day);
            }

            if ("text".equalsIgnoreCase(StringUtils.defaultString(message.getMessageType()))) {
                String content = StringUtils.trimToEmpty(message.getContent());
                if (StringUtils.isNotBlank(content) && !content.startsWith("__SOCIAL_INTENT__:")) {
                    textMessages.add(content);
                }
            }
        }

        String profileText = String.join(" ", Arrays.asList(
                StringUtils.defaultString(user.getSelfIntroduction()),
                StringUtils.defaultString(user.getInterest()),
                StringUtils.defaultString(user.getLoveDeclaration()),
                StringUtils.defaultString(user.getIntro())
        ));
        String chatText = textMessages.stream().limit(120).collect(Collectors.joining(" "));
        String mergedText = (profileText + " " + chatText).trim();

        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(userId);
        List<String> hobbies = resolveHobbies(mergedText, aggregate);
        List<String> strengths = resolveStrengths(sent, received, activeDays.size(), chattedUsers.size(), aggregate);
        String personality = resolvePersonality(sent, received, activeDays.size(), chattedUsers.size());
        String singleDuration = resolveSingleDuration(user, mergedText);

        PersonaReportVO report = new PersonaReportVO();
        report.setReportId("report_" + UUID.randomUUID().toString().replace("-", ""));
        report.setUserId(userId);
        report.setChattedUserCount(chattedUsers.size());
        report.setSentMessageCount(sent);
        report.setReceivedMessageCount(received);
        report.setActiveChatDays(activeDays.size());
        report.setPersonality(personality);
        report.setHobbies(hobbies);
        report.setStrengths(strengths);
        report.setSingleDuration(singleDuration);
        report.setSummary(buildSummary(report));
        report.setGeneratedAt(DATE_TIME_FORMATTER.format(java.time.LocalDateTime.now()));
        report.setReportText(buildReportText(user, report, aggregate));
        return report;
    }

    public PersonaReportVO extractCachedReport(UserPersonaSnapshotEntity snapshot) {
        if (snapshot == null || StringUtils.isBlank(snapshot.getPersonaJson())) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(snapshot.getPersonaJson());
            if (root == null) {
                return null;
            }
            JSONObject cached = root.getJSONObject("report_cache");
            if (cached == null || cached.isEmpty()) {
                return null;
            }
            return cached.toJavaObject(PersonaReportVO.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public PersonaReportVO buildReportFromPersonaJson(Integer userId, JSONObject personaRoot) {
        if (userId == null || personaRoot == null || personaRoot.isEmpty()) {
            return null;
        }
        AppUserEntity user = appUserService.getById(userId);
        if (user == null) {
            throw new LinfengException("用户不存在");
        }
        InteractionStats stats = loadInteractionStats(userId);
        TagProfileAggregateVO aggregate = tagProfileAggregateService.aggregate(userId);

        PersonaReportVO report = new PersonaReportVO();
        report.setReportId("snapshot_" + UUID.randomUUID().toString().replace("-", ""));
        report.setUserId(userId);
        report.setChattedUserCount(stats.chattedUserCount);
        report.setSentMessageCount(stats.sentMessageCount);
        report.setReceivedMessageCount(stats.receivedMessageCount);
        report.setActiveChatDays(stats.activeChatDays);
        report.setPersonality(resolvePersonaPersonality(personaRoot));
        report.setHobbies(resolvePersonaList(personaRoot, "interest_clusters", "tags_highlight", 6));
        report.setStrengths(resolvePersonaStrengths(personaRoot));
        report.setSingleDuration(resolveSingleDuration(user, buildPersonaMergedText(user, stats.textMessages)));
        report.setSummary(StringUtils.defaultIfBlank(personaRoot.getString("summary"), "画像分析完成"));
        report.setGeneratedAt(DATE_TIME_FORMATTER.format(java.time.LocalDateTime.now()));
        report.setReportText(buildPersonaReportText(user, report, aggregate, personaRoot));
        return report;
    }

    private PersonaReportVO buildRuntimeReport(Integer userId) {
        if (agentRuntimeBridgeService == null || !agentRuntimeBridgeService.isPersonaEnabled()) {
            return null;
        }
        AppUserEntity user = appUserService.getById(userId);
        if (user == null) {
            return null;
        }
        JSONObject runtimeReport = agentRuntimeBridgeService.generatePersonaReport(user, user);
        if (runtimeReport == null || runtimeReport.isEmpty()) {
            return null;
        }
        InteractionStats stats = loadInteractionStats(userId);

        PersonaReportVO report = new PersonaReportVO();
        report.setReportId("runtime_" + UUID.randomUUID().toString().replace("-", ""));
        report.setUserId(userId);
        report.setChattedUserCount(stats.chattedUserCount);
        report.setSentMessageCount(stats.sentMessageCount);
        report.setReceivedMessageCount(stats.receivedMessageCount);
        report.setActiveChatDays(stats.activeChatDays);
        report.setPersonality(StringUtils.defaultIfBlank(runtimeReport.getString("emotional_style"), "情绪风格稳定"));
        report.setHobbies(runtimeReport.getJSONArray("interest_clusters") == null
                ? new ArrayList<>()
                : runtimeReport.getJSONArray("interest_clusters").toJavaList(String.class));
        report.setStrengths(extractStrengths(runtimeReport));
        report.setSingleDuration("待结合更多行为样本分析");
        report.setSummary(StringUtils.defaultIfBlank(runtimeReport.getString("summary"), "画像分析完成"));
        report.setGeneratedAt(DATE_TIME_FORMATTER.format(java.time.LocalDateTime.now()));
        report.setReportText(agentRuntimeBridgeService.buildRuntimeReportText(userId, runtimeReport));
        return report;
    }

    private InteractionStats loadInteractionStats(Integer userId) {
        String uidStr = String.valueOf(userId);
        List<ChatMessageEntity> recentMessages = chatMessageService.lambdaQuery()
                .and(wrapper -> wrapper.eq(ChatMessageEntity::getSenderId, uidStr)
                        .or()
                        .eq(ChatMessageEntity::getReceiverId, uidStr))
                .orderByDesc(ChatMessageEntity::getId)
                .last("LIMIT 2000")
                .list();

        int sent = 0;
        int received = 0;
        Set<String> chattedUsers = new HashSet<>();
        Set<LocalDate> activeDays = new HashSet<>();
        List<String> textMessages = new ArrayList<>();
        for (ChatMessageEntity message : recentMessages) {
            if (message == null || Objects.equals(message.getIsWithdrawn(), 1)) {
                continue;
            }
            boolean selfSent = uidStr.equals(StringUtils.trimToEmpty(message.getSenderId()));
            if (selfSent) {
                sent++;
                if (StringUtils.isNotBlank(message.getReceiverId())) {
                    chattedUsers.add(message.getReceiverId());
                }
            } else {
                received++;
                if (StringUtils.isNotBlank(message.getSenderId())) {
                    chattedUsers.add(message.getSenderId());
                }
            }
            LocalDate day = resolveDay(message.getSendTime());
            if (day != null) {
                activeDays.add(day);
            }
            if ("text".equalsIgnoreCase(StringUtils.defaultString(message.getMessageType()))) {
                String content = StringUtils.trimToEmpty(message.getContent());
                if (StringUtils.isNotBlank(content) && !content.startsWith("__SOCIAL_INTENT__:")) {
                    textMessages.add(content);
                }
            }
        }
        return new InteractionStats(sent, received, chattedUsers.size(), activeDays.size(), textMessages);
    }

    private String resolvePersonaPersonality(JSONObject personaRoot) {
        if (personaRoot == null) {
            return "情绪风格稳定";
        }
        String emotionalStyle = StringUtils.defaultIfBlank(personaRoot.getString("emotional_style"), null);
        if (StringUtils.isNotBlank(emotionalStyle)) {
            return emotionalStyle;
        }
        JSONObject socialStyle = personaRoot.getJSONObject("social_style");
        if (socialStyle != null && StringUtils.isNotBlank(socialStyle.getString("online"))) {
            return socialStyle.getString("online");
        }
        JSONObject personality = personaRoot.getJSONObject("personality");
        if (personality != null && StringUtils.isNotBlank(personality.getString("stability"))) {
            return personality.getString("stability");
        }
        return "情绪风格稳定";
    }

    private List<String> resolvePersonaStrengths(JSONObject personaRoot) {
        List<String> strengths = extractStrengths(personaRoot);
        if (!strengths.isEmpty()) {
            return strengths;
        }
        return resolvePersonaList(personaRoot, "approach_suggestions", "suggestions", 4);
    }

    private List<String> resolvePersonaList(JSONObject root, String primaryKey, String fallbackKey, int limit) {
        List<String> values = new ArrayList<>();
        if (root == null) {
            return values;
        }
        JSONArray primary = root.getJSONArray(primaryKey);
        if (primary != null) {
            values.addAll(primary.toJavaList(String.class));
        }
        if (values.isEmpty()) {
            JSONArray fallback = root.getJSONArray(fallbackKey);
            if (fallback != null) {
                values.addAll(fallback.toJavaList(String.class));
            }
        }
        return values.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String buildPersonaMergedText(AppUserEntity user, List<String> textMessages) {
        String profileText = String.join(" ", Arrays.asList(
                StringUtils.defaultString(user.getSelfIntroduction()),
                StringUtils.defaultString(user.getInterest()),
                StringUtils.defaultString(user.getLoveDeclaration()),
                StringUtils.defaultString(user.getIntro())
        ));
        String chatText = textMessages == null ? "" : textMessages.stream().limit(120).collect(Collectors.joining(" "));
        return (profileText + " " + chatText).trim();
    }

    private String buildPersonaReportText(AppUserEntity user,
                                          PersonaReportVO report,
                                          TagProfileAggregateVO aggregate,
                                          JSONObject personaRoot) {
        StringBuilder builder = new StringBuilder();
        builder.append("# AI画像报告 V2\n");
        builder.append("- 用户ID：").append(report.getUserId()).append("\n");
        builder.append("- 生成时间：").append(StringUtils.defaultIfBlank(report.getGeneratedAt(), DATE_TIME_FORMATTER.format(java.time.LocalDateTime.now()))).append("\n\n");
        builder.append("## 总结\n");
        builder.append(StringUtils.defaultIfBlank(report.getSummary(), "暂无总结")).append("\n\n");
        builder.append("## 情感与关系\n");
        builder.append("- 情绪风格：").append(StringUtils.defaultIfBlank(report.getPersonality(), "暂无")).append("\n");
        builder.append("- 依恋倾向：").append(resolveAttachmentStyle(personaRoot)).append("\n\n");
        builder.append("## 兴趣簇\n");
        builder.append("- ").append(String.join("、", defaultList(report.getHobbies()))).append("\n\n");
        builder.append("## 特长优势\n");
        builder.append("- ").append(String.join("；", defaultList(report.getStrengths()))).append("\n\n");
        builder.append("## 风险提示\n");
        builder.append("- ").append(String.join("；", resolvePersonaList(personaRoot, "risk_flags", "risk_points", 4))).append("\n\n");
        builder.append("## 建议\n");
        builder.append("- ").append(String.join("；", resolvePersonaList(personaRoot, "approach_suggestions", "suggestions", 4))).append("\n\n");
        builder.append("## 标签线索\n");
        builder.append("- ").append(String.join("、", buildEvidenceTags(user, aggregate, personaRoot))).append("\n");
        return builder.toString();
    }

    private String resolveAttachmentStyle(JSONObject personaRoot) {
        if (personaRoot == null) {
            return "暂无";
        }
        String attachmentStyle = StringUtils.defaultIfBlank(personaRoot.getString("attachment_style"), null);
        if (StringUtils.isNotBlank(attachmentStyle)) {
            return attachmentStyle;
        }
        JSONObject loveStyle = personaRoot.getJSONObject("love_style");
        if (loveStyle != null && StringUtils.isNotBlank(loveStyle.getString("attitude"))) {
            return loveStyle.getString("attitude");
        }
        return "暂无";
    }

    private List<String> buildEvidenceTags(AppUserEntity user, TagProfileAggregateVO aggregate, JSONObject personaRoot) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (user != null) {
            if (StringUtils.isNotBlank(user.getCity())) {
                values.add(user.getCity());
            }
            if (StringUtils.isNotBlank(user.getJob())) {
                values.add(user.getJob());
            }
        }
        if (aggregate != null) {
            values.addAll(defaultList(aggregate.getSelfTags()));
            values.addAll(defaultList(aggregate.getBehaviorTags()));
            values.addAll(defaultList(aggregate.getImpressionTop()));
        }
        values.addAll(resolvePersonaList(personaRoot, "interest_clusters", "tags_highlight", 6));
        return values.stream().filter(StringUtils::isNotBlank).limit(8).collect(Collectors.toList());
    }

    private List<String> defaultList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.singletonList("暂无");
        }
        List<String> values = source.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toList());
        return values.isEmpty() ? Collections.singletonList("暂无") : values;
    }

    private List<String> extractStrengths(JSONObject runtimeReport) {
        List<String> strengths = new ArrayList<>();
        if (runtimeReport == null) {
            return strengths;
        }
        List<JSONObject> traits = runtimeReport.getJSONArray("core_traits") == null
                ? Collections.emptyList()
                : runtimeReport.getJSONArray("core_traits").toJavaList(JSONObject.class);
        for (JSONObject trait : traits) {
            if (trait == null) {
                continue;
            }
            String name = trait.getString("name");
            Integer score = trait.getInteger("score");
            if (StringUtils.isBlank(name) || score == null) {
                continue;
            }
            if (score >= 60) {
                strengths.add(name + "较强");
            }
        }
        if (strengths.isEmpty()) {
            strengths.addAll(runtimeReport.getJSONArray("approach_suggestions") == null
                    ? Collections.singletonList("关系推进建议较明确")
                    : runtimeReport.getJSONArray("approach_suggestions").toJavaList(String.class).stream().limit(3).collect(Collectors.toList()));
        }
        return strengths.stream().limit(6).collect(Collectors.toList());
    }

    private static final class InteractionStats {
        private final int sentMessageCount;
        private final int receivedMessageCount;
        private final int chattedUserCount;
        private final int activeChatDays;
        private final List<String> textMessages;

        private InteractionStats(int sentMessageCount,
                                 int receivedMessageCount,
                                 int chattedUserCount,
                                 int activeChatDays,
                                 List<String> textMessages) {
            this.sentMessageCount = sentMessageCount;
            this.receivedMessageCount = receivedMessageCount;
            this.chattedUserCount = chattedUserCount;
            this.activeChatDays = activeChatDays;
            this.textMessages = textMessages == null ? Collections.emptyList() : textMessages;
        }
    }

    private LocalDate resolveDay(String sendTime) {
        if (StringUtils.isBlank(sendTime)) {
            return null;
        }
        String value = sendTime.trim();
        try {
            if (value.matches("^\\d{10,13}$")) {
                long ts = Long.parseLong(value);
                if (value.length() == 10) {
                    ts = ts * 1000L;
                }
                return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private List<String> resolveHobbies(String mergedText, TagProfileAggregateVO aggregate) {
        LinkedHashSet<String> hobbies = new LinkedHashSet<>();
        String text = StringUtils.defaultString(mergedText).toLowerCase(Locale.ROOT);

        Map<String, String> dict = new LinkedHashMap<>();
        dict.put("旅行", "旅行探索");
        dict.put("跑步", "运动健身");
        dict.put("健身", "运动健身");
        dict.put("瑜伽", "瑜伽舒展");
        dict.put("篮球", "球类运动");
        dict.put("羽毛球", "球类运动");
        dict.put("阅读", "阅读写作");
        dict.put("看书", "阅读写作");
        dict.put("摄影", "摄影记录");
        dict.put("拍照", "摄影记录");
        dict.put("电影", "观影观展");
        dict.put("音乐", "音乐聆听");
        dict.put("唱歌", "音乐表达");
        dict.put("美食", "美食探索");
        dict.put("做饭", "烹饪手作");
        dict.put("宠物", "宠物陪伴");
        dict.put("猫", "宠物陪伴");
        dict.put("狗", "宠物陪伴");

        for (Map.Entry<String, String> entry : dict.entrySet()) {
            if (text.contains(entry.getKey())) {
                hobbies.add(entry.getValue());
            }
            if (hobbies.size() >= 6) {
                break;
            }
        }

        aggregate.getSelfTags().stream().limit(4).forEach(hobbies::add);
        if (hobbies.isEmpty()) {
            hobbies.add("认真交友");
            hobbies.add("真实沟通");
        }
        return new ArrayList<>(hobbies).stream().limit(6).collect(Collectors.toList());
    }

    private List<String> resolveStrengths(int sent, int received, int activeDays, int chattedUsers, TagProfileAggregateVO aggregate) {
        LinkedHashSet<String> strengths = new LinkedHashSet<>();
        if (sent + received >= 80) {
            strengths.add("沟通投入度高");
        }
        if (activeDays >= 10) {
            strengths.add("社交节奏稳定");
        }
        if (chattedUsers >= 8) {
            strengths.add("社交拓展能力强");
        }
        if (Math.abs(sent - received) <= Math.max(8, (sent + received) / 5)) {
            strengths.add("表达与倾听平衡");
        }
        if (!aggregate.getImpressionTop().isEmpty()) {
            strengths.add("他人印象标签清晰");
        }
        if (!aggregate.getBehaviorTags().isEmpty()) {
            strengths.add("行为画像信号稳定");
        }
        if (strengths.isEmpty()) {
            strengths.add("真实表达");
            strengths.add("愿意沟通");
        }
        return new ArrayList<>(strengths).stream().limit(6).collect(Collectors.toList());
    }

    private String resolvePersonality(int sent, int received, int activeDays, int chattedUsers) {
        int total = sent + received;
        if (total <= 0) {
            return "资料型慢热，线上互动样本较少";
        }
        if (sent > received * 1.3) {
            if (activeDays >= 10) {
                return "主动外向，擅长带动聊天氛围";
            }
            return "表达主动，破冰意愿较强";
        }
        if (received > sent * 1.3) {
            if (chattedUsers >= 6) {
                return "倾听耐心，容易让对方愿意倾诉";
            }
            return "温和慢热，更偏好深聊";
        }
        if (activeDays >= 12 && chattedUsers >= 8) {
            return "社交稳定，互动节奏健康";
        }
        return "理性平衡，聊天风格稳健";
    }

    private String resolveSingleDuration(AppUserEntity user, String mergedText) {
        Matcher matcher = SINGLE_PATTERN.matcher(StringUtils.defaultString(mergedText));
        if (matcher.find()) {
            return "约" + matcher.group(1) + matcher.group(2);
        }
        if (Objects.equals(user.getMarryStatus(), 0)) {
            return "未婚（未填写具体单身时长）";
        }
        if (Objects.equals(user.getMarryStatus(), 1)) {
            return "离异（未填写空窗时长）";
        }
        if (Objects.equals(user.getMarryStatus(), 2)) {
            return "丧偶（未填写空窗时长）";
        }
        return "未填写";
    }

    private String buildSummary(PersonaReportVO report) {
        return "最近聊过" + report.getChattedUserCount() + "人，活跃" + report.getActiveChatDays()
                + "天，整体呈现“" + report.getPersonality() + "”特征。";
    }

    private String buildReportText(AppUserEntity user, PersonaReportVO report, TagProfileAggregateVO aggregate) {
        StringBuilder builder = new StringBuilder();
        builder.append("# AI画像报告\n");
        builder.append("- 用户：").append(StringUtils.defaultIfBlank(user.getUsername(), "匿名用户")).append("\n");
        builder.append("- 生成时间：").append(report.getGeneratedAt()).append("\n\n");

        builder.append("## 社交数据\n");
        builder.append("- 聊过人数：").append(report.getChattedUserCount()).append("\n");
        builder.append("- 发送消息：").append(report.getSentMessageCount()).append(" 条\n");
        builder.append("- 接收消息：").append(report.getReceivedMessageCount()).append(" 条\n");
        builder.append("- 活跃聊天天数：").append(report.getActiveChatDays()).append(" 天\n\n");

        builder.append("## 画像解读\n");
        builder.append("- 性格倾向：").append(report.getPersonality()).append("\n");
        builder.append("- 爱好：").append(String.join("、", report.getHobbies())).append("\n");
        builder.append("- 特长：").append(String.join("、", report.getStrengths())).append("\n");
        builder.append("- 单身时长：").append(report.getSingleDuration()).append("\n\n");

        builder.append("## 标签画像\n");
        builder.append("- 自选标签：").append(joinOrDefault(aggregate.getSelfTags(), "暂无")).append("\n");
        builder.append("- 他人印象：").append(joinOrDefault(aggregate.getImpressionTop(), "暂无")).append("\n");
        builder.append("- 行为标签：").append(joinOrDefault(aggregate.getBehaviorTags(), "暂无")).append("\n\n");

        builder.append("## 总结\n");
        builder.append(report.getSummary()).append("\n");

        return builder.toString();
    }

    private String joinOrDefault(List<String> list, String fallback) {
        if (list == null || list.isEmpty()) {
            return fallback;
        }
        return String.join("、", list);
    }
}
