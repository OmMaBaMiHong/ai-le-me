package org.aileme.tools;

import org.apache.commons.lang3.StringUtils;
import org.aileme.AilemeApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.app.oss.factory.RuoyiSysClound;
import org.aileme.shejiao.app.service.ai.image.AIImageStrategyManager;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DirectUserFigureGenerateRunner {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    private static final String[] FEMALE_SCENES = {
        "傍晚公园散步时顺手拿着相机记录风景",
        "周末在街角咖啡馆靠窗坐着整理照片",
        "下班后在健身房拉伸放松，刚结束训练",
        "周末在城市绿道徒步途中回头看向镜头",
        "在烘焙教室或家里厨房完成甜点后自然站立",
        "在江边步道慢走，迎着自然侧光"
    };

    private static final String[] MALE_SCENES = {
        "傍晚在城市篮球场边休息喝水",
        "周末在咖啡馆外带相机准备扫街拍照",
        "下班后在健身房训练结束后自然站立",
        "周末在河边绿道慢跑后放松",
        "在书店或共享办公空间翻看一本书",
        "在城市公园长椅旁整理随身背包"
    };

    private static final String[] SHOT_VARIANTS = {
        "中景抓拍",
        "全身生活照",
        "半身侧拍",
        "三分之二身位的纪实镜头",
        "轻微仰拍的日常摄影",
        "带环境信息的人像镜头"
    };

    private static final String[] OUTFIT_VARIANTS = {
        "休闲通勤穿搭",
        "轻运动风穿搭",
        "简洁干净的周末私服",
        "带一点城市户外感的穿搭",
        "自然不刻意的日常服装",
        "适合真实拍摄的人像穿搭"
    };

    private static final String[] EXPRESSION_VARIANTS = {
        "表情放松自然",
        "带一点轻松微笑",
        "目光平静但有交流感",
        "像被朋友随手拍到的自然神态",
        "不要摆拍感，状态真实",
        "情绪稳定，松弛自然"
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("server.port", "0");
        System.setProperty("spring.boot.admin.client.enabled", "false");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AilemeApplication.class).run(args)) {
            int uid = intProp("figure.uid", -1);
            int imageCount = clamp(intProp("figure.count", 1), 1, 3);
            int uidStart = intProp("figure.uidStart", 1);
            int uidEnd = intProp("figure.uidEnd", Integer.MAX_VALUE);
            int userLimit = intProp("figure.userLimit", Integer.MAX_VALUE);
            String uidList = StringUtils.defaultString(System.getProperty("figure.uidList"));
            boolean overwrite = boolProp("figure.overwrite", true);

            AppUserService appUserService = context.getBean(AppUserService.class);
            AIImageStrategyManager imageStrategyManager = context.getBean(AIImageStrategyManager.class);
            RuoyiSysClound cloudStorage = new RuoyiSysClound();

            List<AppUserEntity> users = loadUsers(appUserService, uid, uidList, uidStart, uidEnd, userLimit, overwrite);
            if (users.isEmpty()) {
                System.out.println("DIRECT_USER_FIGUR_BATCH_RESULT=" + Map.of(
                    "requestedUid", uid,
                    "userCount", 0,
                    "message", "没有可处理的用户"
                ));
                return;
            }

            int success = 0;
            int skipped = 0;
            List<Map<String, Object>> failed = new ArrayList<>();
            List<Map<String, Object>> completed = new ArrayList<>();

            for (AppUserEntity user : users) {
                try {
                    if (StringUtils.isBlank(user.getAvatar())) {
                        skipped++;
                        failed.add(failureItem(user, "头像为空"));
                        continue;
                    }
                    List<String> uploadedUrls = generateAndUploadUserImages(user, imageCount, imageStrategyManager, cloudStorage);
                    user.setFigur(String.join(",", uploadedUrls));
                    user.setUpdateTime(new Date());
                    appUserService.updateById(user);
                    success++;
                    completed.add(Map.of(
                        "uid", user.getUid(),
                        "username", StringUtils.defaultString(user.getUsername()),
                        "figurCount", uploadedUrls.size(),
                        "figur", uploadedUrls
                    ));
                    System.out.println("DIRECT_USER_FIGUR_RESULT=" + Map.of(
                        "uid", user.getUid(),
                        "username", StringUtils.defaultString(user.getUsername()),
                        "figur", uploadedUrls
                    ));
                } catch (Exception ex) {
                    String reason = StringUtils.defaultIfBlank(ex.getMessage(), ex.getClass().getSimpleName());
                    failed.add(failureItem(user, reason));
                    System.err.println("DIRECT_USER_FIGUR_FAILED uid=" + user.getUid() + ", reason=" + reason);
                }
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("requestedUid", uid);
            summary.put("requestedImageCount", imageCount);
            summary.put("processedUsers", users.size());
            summary.put("successUsers", success);
            summary.put("skippedUsers", skipped);
            summary.put("failedUsers", failed.size());
            summary.put("completed", completed);
            summary.put("failed", failed);
            System.out.println("DIRECT_USER_FIGUR_BATCH_RESULT=" + summary);
        }
    }

    private static List<AppUserEntity> loadUsers(
        AppUserService appUserService,
        int uid,
        String uidList,
        int uidStart,
        int uidEnd,
        int userLimit,
        boolean overwrite
    ) {
        if (uid > 0) {
            AppUserEntity user = appUserService.getById(uid);
            if (user == null) {
                return List.of();
            }
            if (!overwrite && StringUtils.isNotBlank(user.getFigur())) {
                return List.of();
            }
            return List.of(user);
        }
        List<Integer> explicitUids = parseUidList(uidList);
        if (!explicitUids.isEmpty()) {
            return explicitUids.stream()
                .map(appUserService::getById)
                .filter(java.util.Objects::nonNull)
                .filter(user -> overwrite || StringUtils.isBlank(user.getFigur()))
                .toList();
        }
        return appUserService.lambdaQuery()
            .ge(AppUserEntity::getUid, Math.max(1, uidStart))
            .le(AppUserEntity::getUid, uidEnd)
            .orderByAsc(AppUserEntity::getUid)
            .list()
            .stream()
            .filter(user -> overwrite || StringUtils.isBlank(user.getFigur()))
            .limit(Math.max(1, userLimit))
            .toList();
    }

    private static List<Integer> parseUidList(String uidList) {
        if (StringUtils.isBlank(uidList)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (String part : uidList.split(",")) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed uid items so a single bad value does not block the batch.
            }
        }
        return result;
    }

    private static List<String> generateAndUploadUserImages(
        AppUserEntity user,
        int imageCount,
        AIImageStrategyManager imageStrategyManager,
        RuoyiSysClound cloudStorage
    ) throws Exception {
        byte[] avatarBytes = downloadBytes(user.getAvatar());
        String referenceUrl = uploadBytes(cloudStorage, avatarBytes, "jpg");
        List<String> uploadedUrls = new ArrayList<>();
        for (int index = 0; index < imageCount; index++) {
            final int lane = index;
            String prompt = buildPrompt(user, lane);
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("max_images", 1);
            params.put("size", "2K");
            params.put("timeout", 180);
            params.put("watermark", false);
            params.put("function_type", "direct_user_figur_generate");
            params.put("scene_code", "user_figur_" + lane);

            List<String> generated = imageStrategyManager.generateImages(prompt, List.of(referenceUrl), params);
            String generatedUrl = generated.stream().filter(StringUtils::isNotBlank).findFirst()
                .orElseThrow(() -> new IllegalStateException("生成结果为空, uid=" + user.getUid() + ", index=" + lane));
            byte[] generatedBytes = downloadBytes(generatedUrl);
            uploadedUrls.add(uploadBytes(cloudStorage, generatedBytes, inferSuffix(generatedUrl)));
        }
        if (uploadedUrls.isEmpty()) {
            throw new IllegalStateException("图片生成成功但上传 OSS 后为空");
        }
        return uploadedUrls;
    }

    private static String buildPrompt(AppUserEntity user, int index) {
        String gender = user.getGender() != null && user.getGender() == 2 ? "亚洲女性" : "亚洲男性";
        String city = StringUtils.defaultIfBlank(user.getCity(), "中国城市");
        String job = StringUtils.defaultIfBlank(user.getJob(), "职场人士");
        String interests = StringUtils.defaultIfBlank(user.getInterest(), "运动、阅读、旅行");
        String intro = StringUtils.defaultIfBlank(user.getSelfIntroduction(), StringUtils.defaultIfBlank(user.getIntro(), "性格真实自然，生活化"));
        String declaration = StringUtils.defaultIfBlank(user.getLoveDeclaration(), "期待稳定真诚的关系");
        int age = user.getAge() == null ? 26 : user.getAge();
        String scene = pickVariant(user, index, user.getGender() != null && user.getGender() == 2 ? FEMALE_SCENES : MALE_SCENES);
        String shot = pickVariant(user, index + 3, SHOT_VARIANTS);
        String outfit = pickVariant(user, index + 7, OUTFIT_VARIANTS);
        String expression = pickVariant(user, index + 11, EXPRESSION_VARIANTS);
        return "基于参考图生成同一个人的全新生活照，必须保持同一张脸、同一人物身份特征，但绝对不能直接复用原头像的构图、背景、表情、裁切、手势和姿势。"
            + gender + "，" + age + "岁，常住" + city + "，职业是" + job + "。"
            + "人物气质：" + trimText(intro, 90) + "。"
            + "情感偏好：" + trimText(declaration, 70) + "。"
            + "兴趣爱好：" + trimText(interests, 70) + "。"
            + "本次场景设定：" + scene + "。"
            + "镜头要求：" + shot + "，" + outfit + "，" + expression + "。"
            + "场景要和人物职业、城市与兴趣匹配，像真正新拍的一张高质量纪实生活照。"
            + "人物必须是单人亚洲面孔，真实写实摄影，不要证件照，不要棚拍感，不要拼贴感，不要重复原头像动作，不要文字，不要水印。";
    }

    private static String pickVariant(AppUserEntity user, int index, String[] variants) {
        int uid = user.getUid() == null ? 1 : user.getUid();
        return variants[Math.floorMod(uid * 17 + index * 31, variants.length)];
    }

    private static String trimText(String text, int maxLength) {
        String value = StringUtils.normalizeSpace(StringUtils.defaultString(text));
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static Map<String, Object> failureItem(AppUserEntity user, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("uid", user == null ? null : user.getUid());
        item.put("username", user == null ? "" : StringUtils.defaultString(user.getUsername()));
        item.put("reason", StringUtils.defaultIfBlank(reason, "未知错误"));
        return item;
    }

    private static byte[] downloadBytes(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", "https://image.baidu.com/")
            .GET()
            .build();
        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body() == null || response.body().length == 0) {
            throw new IllegalStateException("下载失败: " + response.statusCode() + ", url=" + url);
        }
        return response.body();
    }

    private static String uploadBytes(RuoyiSysClound cloudStorage, byte[] bytes, String suffix) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return cloudStorage.uploadSuffix(inputStream, StringUtils.defaultIfBlank(suffix, "jpg"));
        }
    }

    private static String inferSuffix(String url) {
        String lower = StringUtils.defaultString(url).toLowerCase();
        if (lower.contains(".png")) {
            return "png";
        }
        if (lower.contains(".webp")) {
            return "webp";
        }
        return "jpg";
    }

    private static int intProp(String key, int defaultValue) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static boolean boolProp(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(System.getProperty(key, String.valueOf(defaultValue)));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
