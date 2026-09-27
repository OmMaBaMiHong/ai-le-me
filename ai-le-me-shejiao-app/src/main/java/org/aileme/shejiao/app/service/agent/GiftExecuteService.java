package org.aileme.shejiao.app.service.agent;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AccountService;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.BillService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.PostService;
import org.aileme.shejiao.api.service.SysConfigService;
import org.aileme.shejiao.api.service.UserLevelService;
import org.aileme.shejiao.app.dao.GiftTaskDao;
import org.aileme.shejiao.app.service.impl.SocialIntentMessageService;
import org.aileme.shejiao.app.service.ai.image.AIImageStrategyManager;
import org.aileme.shejiao.app.oss.factory.OSSFactory;
import org.aileme.shejiao.common.enums.BillDetailEnum;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.Constant;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import org.aileme.shejiao.domain.entity.app.GiftTaskEntity;
import org.aileme.shejiao.domain.param.app.AgentGiftExecuteForm;
import org.aileme.shejiao.domain.vo.AgentGiftExecuteVo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@DS("master")
@Service
public class GiftExecuteService {

    public static final int TASK_STATUS_PENDING = 0;
    public static final int TASK_STATUS_SUCCESS = 1;
    public static final int TASK_STATUS_FAILED = 2;
    public static final int TASK_STATUS_PROCESSING = 3;
    public static final int GIFT_STATUS_PENDING_ACCEPT = 0;
    public static final int GIFT_STATUS_ACCEPTED = 1;
    public static final int GIFT_STATUS_REJECTED = 2;
    public static final int GIFT_STATUS_EXPIRED = 3;
    private static final int MAX_SOCIAL_INTENT_MESSAGE_LENGTH = 255;

    private final HttpClient imageDownloadClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private PostService postService;

    @Autowired
    private BillService billService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private FriendService friendService;

    @Autowired
    private GiftTaskDao giftTaskDao;

    @Autowired
    private SocialIntentMessageService socialIntentMessageService;

    @Autowired
    private UserLevelService userLevelService;

    @Autowired
    private SysConfigService configService;

    @Autowired(required = false)
    private AIImageStrategyManager imageStrategyManager;

    @DSTransactional
    public AgentGiftExecuteVo execute(AppUserEntity currentUser, AgentGiftExecuteForm form) {
        AppUserEntity sender = requireUser(currentUser.getUid(), "当前用户不存在");
        AppUserEntity target = requireUser(form.getTargetUid(), "目标用户不存在");
        PostEntity post = resolveOptionalPost(form.getPostId());

        if (Objects.equals(sender.getUid(), target.getUid())) {
            throw new LinfengException("不能给自己送礼物");
        }
        if (post != null && !Objects.equals(post.getUid(), target.getUid())) {
            throw new LinfengException("礼物目标和帖子作者不一致");
        }
        Long resolvedSessionId = friendService.getOrCreateSession(sender.getUid(), target.getUid());
        if (resolvedSessionId == null || resolvedSessionId <= 0) {
            throw new LinfengException("会话不存在或已失效");
        }
        form.setSessionId(String.valueOf(resolvedSessionId));
        accountService.freezeCoin(sender.getUid(), form.getAmount());

        List<String> referenceMedia = collectReferenceMedia(sender, target, 4);
        GiftTaskEntity task = buildTask(sender, target, post, form, referenceMedia);
        giftTaskDao.insert(task);
        sendGiftTipMessage(task);

        return toExecuteVo(task, accountService.getCoinBalance(sender.getUid()).intValue());
    }

    public AgentGiftExecuteVo getTaskDetail(AppUserEntity currentUser, Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new LinfengException("礼物任务不存在");
        }
        GiftTaskEntity task = giftTaskDao.selectById(taskId);
        if (task == null) {
            throw new LinfengException("礼物任务不存在");
        }
        Integer currentUid = currentUser == null ? null : currentUser.getUid();
        if (!Objects.equals(task.getSenderUid(), currentUid) && !Objects.equals(task.getTargetUid(), currentUid)) {
            throw new LinfengException("无权查看该礼物任务");
        }
        int balance = 0;
        if (Objects.equals(task.getSenderUid(), currentUid)) {
            balance = accountService.getCoinBalance(currentUid).intValue();
        }
        return toExecuteVo(task, balance);
    }

    @DSTransactional
    public AgentGiftExecuteVo confirmGift(AppUserEntity currentUser, String requestId, Integer requesterUid, String sessionId) {
        GiftTaskEntity task = requireTask(requestId, requesterUid, currentUser.getUid(), sessionId);
        if (task.getGiftStatus() != null && task.getGiftStatus() == GIFT_STATUS_ACCEPTED) {
            return toExecuteVo(task, 0);
        }
        if (task.getGiftStatus() != null && task.getGiftStatus() == GIFT_STATUS_REJECTED) {
            throw new LinfengException("该礼物已被婉拒");
        }
        if (task.getGiftStatus() != null && task.getGiftStatus() == GIFT_STATUS_EXPIRED) {
            throw new LinfengException("该礼物已过期");
        }

        AppUserEntity sender = requireUser(task.getSenderUid(), "送礼用户不存在");
        AppUserEntity target = requireUser(task.getTargetUid(), "收礼用户不存在");
        PostEntity post = resolveOptionalPost(task.getPostId());

        int amount = Math.max(0, task.getRewardAmount() == null ? 0 : task.getRewardAmount());
        int feeAmount = 0;
        int arrivalAmount = amount;
        BigDecimal commissionRate = parseCommissionRate(configService.getValue(Constant.REWARD_COMMISSION_RATE));
        if (amount > 0 && commissionRate.compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = BigDecimal.valueOf(amount)
                    .multiply(commissionRate)
                    .divide(new BigDecimal("100"), 0, java.math.RoundingMode.DOWN)
                    .intValue();
            if (feeAmount >= amount) {
                feeAmount = amount - 1;
            }
            arrivalAmount = amount - feeAmount;
        }

        accountService.consumeFrozenCoin(sender.getUid(), amount);
        accountService.increaseCoin(target.getUid(), arrivalAmount);
        userLevelService.checkUserLevel(sender.getUid());
        userLevelService.checkUserLevel(target.getUid());

        String payerMark = feeAmount > 0
                ? "礼物破冰接受扣除积分（含平台服务费" + feeAmount + "积分）"
                : "礼物破冰接受扣除积分";
        String receiverMark = feeAmount > 0
                ? "礼物破冰接受增加积分（已扣除平台服务费" + feeAmount + "积分）"
                : "礼物破冰接受增加积分";
        billService.expend(
                sender.getUid(),
                BillDetailEnum.TYPE_20.getDesc(),
                BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_20.getValue(),
                amount,
                accountService.getCoinBalance(sender.getUid()).doubleValue(),
                payerMark,
                resolveBillLinkId(task, post),
                target.getUid()
        );
        billService.income(
                target.getUid(),
                BillDetailEnum.TYPE_19.getDesc(),
                BillDetailEnum.CATEGORY_2.getValue(),
                BillDetailEnum.TYPE_19.getValue(),
                arrivalAmount,
                accountService.getCoinBalance(target.getUid()).doubleValue(),
                receiverMark,
                resolveBillLinkId(task, post),
                sender.getUid()
        );

        Long activatedSessionId = friendService.activateFriendship(sender.getUid(), target.getUid());
        task.setSessionId(String.valueOf(activatedSessionId));
        task.setGiftStatus(GIFT_STATUS_ACCEPTED);
        task.setTaskStatus(TASK_STATUS_PENDING);
        task.setStatusNote("对方已接受，礼物图生成排队中");
        task.setUpdateTime(new Date());
        giftTaskDao.updateById(task);
        syncGiftTipMessageState(task);
        return toExecuteVo(task, 0);
    }

    @DSTransactional
    public AgentGiftExecuteVo rejectGift(AppUserEntity currentUser, String requestId, Integer requesterUid, String sessionId) {
        GiftTaskEntity task = requireTask(requestId, requesterUid, currentUser.getUid(), sessionId);
        if (task.getGiftStatus() != null && task.getGiftStatus() == GIFT_STATUS_REJECTED) {
            return toExecuteVo(task, 0);
        }
        if (task.getGiftStatus() != null && task.getGiftStatus() == GIFT_STATUS_ACCEPTED) {
            throw new LinfengException("该礼物已被接受");
        }
        if (task.getGiftStatus() != null && task.getGiftStatus() == GIFT_STATUS_EXPIRED) {
            return toExecuteVo(task, 0);
        }
        accountService.unfreezeCoin(task.getSenderUid(), Math.max(0, task.getRewardAmount() == null ? 0 : task.getRewardAmount()));
        task.setGiftStatus(GIFT_STATUS_REJECTED);
        task.setStatusNote("对方已婉拒，未扣款");
        task.setUpdateTime(new Date());
        giftTaskDao.updateById(task);
        syncGiftTipMessageState(task);
        return toExecuteVo(task, 0);
    }

    @DSTransactional
    public boolean expireTask(Long taskId) {
        GiftTaskEntity task = giftTaskDao.selectById(taskId);
        if (task == null || task.getGiftStatus() == null || task.getGiftStatus() != GIFT_STATUS_PENDING_ACCEPT) {
            return true;
        }
        accountService.unfreezeCoin(task.getSenderUid(), Math.max(0, task.getRewardAmount() == null ? 0 : task.getRewardAmount()));
        task.setGiftStatus(GIFT_STATUS_EXPIRED);
        task.setStatusNote("礼物申请已过期，未扣款");
        task.setUpdateTime(new Date());
        giftTaskDao.updateById(task);
        syncGiftTipMessageState(task);
        return true;
    }

    @DSTransactional
    public boolean processTask(Long taskId) {
        GiftTaskEntity task = giftTaskDao.selectById(taskId);
        if (task == null) {
            return true;
        }
        if (task.getGiftStatus() == null || task.getGiftStatus() != GIFT_STATUS_ACCEPTED) {
            return true;
        }
        if (isFinalStatus(task.getTaskStatus())) {
            return true;
        }

        task.setTaskStatus(TASK_STATUS_PROCESSING);
        task.setStatusNote("心意已送达，礼物图生成中");
        task.setUpdateTime(new Date());
        giftTaskDao.updateById(task);
        syncGiftTipMessageState(task);

        List<String> referenceMedia = parseStringList(task.getReferenceMedia());
        List<String> generatedMedia = Collections.emptyList();
        int taskStatus = TASK_STATUS_FAILED;
        String taskMessage = "心意已送达，但礼物图生成失败";
        try {
            AgentGiftExecuteForm form = rebuildForm(task);
            generatedMedia = persistGeneratedImages(tryGenerateGiftImages(form, referenceMedia));
            if (!generatedMedia.isEmpty()) {
                taskStatus = TASK_STATUS_SUCCESS;
                taskMessage = "心意已送达，礼物图已生成";
            } else {
                taskMessage = "心意已送达，但礼物图暂未生成成功";
            }
        } catch (Exception ex) {
            log.warn("[gift-execute] async generate failed. taskId={}, senderUid={}, targetUid={}, reason={}",
                    task.getId(), task.getSenderUid(), task.getTargetUid(), ex.getMessage());
        }

        task.setGeneratedMedia(JSON.toJSONString(generatedMedia));
        task.setTaskStatus(taskStatus);
        task.setStatusNote(limitText(taskMessage, 120));
        task.setUpdateTime(new Date());
        giftTaskDao.updateById(task);
        syncGiftTipMessageState(task);
        return true;
    }

    private GiftTaskEntity buildTask(AppUserEntity sender,
                                     AppUserEntity target,
                                     PostEntity post,
                                     AgentGiftExecuteForm form,
                                     List<String> referenceMedia) {
        Date now = new Date();
        GiftTaskEntity task = new GiftTaskEntity();
        task.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        task.setSenderUid(sender.getUid());
        task.setTargetUid(target.getUid());
        task.setPostId(post == null ? null : post.getId());
        task.setSessionId(limitText(form.getSessionId(), 64));
        task.setRewardAmount(form.getAmount());
        task.setGiftCode(limitText(form.getGiftCode(), 32));
        task.setGiftName(limitText(form.getGiftName(), 40));
        task.setGiftIcon(limitText(form.getGiftIcon(), 8));
        task.setGiftScene(limitText(form.getGiftScene(), 32));
        task.setGiftTheme(limitText(form.getGiftTheme(), 24));
        task.setTemplateCode(limitText(form.getTemplateCode(), 32));
        task.setAnimationPreset(limitText(form.getAnimationPreset(), 32));
        task.setRevealEffect(limitText(form.getRevealEffect(), 32));
        task.setSoundEffectKey(limitText(form.getSoundEffectKey(), 32));
        task.setSoundEffectUrl(limitText(form.getSoundEffectUrl(), 255));
        task.setRelationshipStage(limitText(form.getRelationshipStage(), 24));
        task.setProvider(limitText(form.getProvider(), 40));
        task.setStrategyNote(limitText(form.getStrategyNote(), 255));
        task.setReason(limitText(form.getReason(), 255));
        task.setNote(limitText(form.getNote(), 255));
        task.setVisualPrompt(limitText(firstNonBlank(form.getVisualPrompt(), buildFallbackVisualPrompt(sender, target, form)), 1000));
        task.setMotionPrompt(limitText(firstNonBlank(form.getMotionPrompt(), ""), 1000));
        task.setReferenceMedia(JSON.toJSONString(referenceMedia));
        task.setGeneratedMedia("[]");
        task.setGiftStatus(GIFT_STATUS_PENDING_ACCEPT);
        task.setTaskStatus(TASK_STATUS_PENDING);
        task.setStatusNote("等待对方接受后扣款并解锁礼物");
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    private AgentGiftExecuteVo toExecuteVo(GiftTaskEntity task, int currentBalance) {
        List<String> assetUrls = parseStringList(task.getGeneratedMedia());
        return AgentGiftExecuteVo.builder()
                .taskId(task.getId())
                .requestId(task.getRequestId())
                .amount(task.getRewardAmount())
                .giftName(limitText(task.getGiftName(), 40))
                .templateCode(limitText(task.getTemplateCode(), 32))
                .animationPreset(limitText(task.getAnimationPreset(), 32))
                .revealEffect(limitText(task.getRevealEffect(), 32))
                .soundEffectKey(limitText(task.getSoundEffectKey(), 32))
                .soundEffectUrl(limitText(task.getSoundEffectUrl(), 255))
                .sessionId(limitText(task.getSessionId(), 64))
                .businessStatus(toGiftStatusText(task.getGiftStatus()))
                .taskStatus(toStatusText(task.getTaskStatus()))
                .taskMessage(limitText(task.getStatusNote(), 120))
                .assetUrls(assetUrls)
                .currentBalance(currentBalance)
                .generated(!assetUrls.isEmpty())
                .build();
    }

    private AgentGiftExecuteForm rebuildForm(GiftTaskEntity task) {
        AgentGiftExecuteForm form = new AgentGiftExecuteForm();
        form.setTargetUid(task.getTargetUid());
        form.setPostId(task.getPostId());
        form.setAmount(task.getRewardAmount());
        form.setGiftCode(task.getGiftCode());
        form.setGiftName(task.getGiftName());
        form.setGiftIcon(task.getGiftIcon());
        form.setGiftScene(task.getGiftScene());
        form.setGiftTheme(task.getGiftTheme());
        form.setTemplateCode(task.getTemplateCode());
        form.setAnimationPreset(task.getAnimationPreset());
        form.setRevealEffect(task.getRevealEffect());
        form.setSoundEffectKey(task.getSoundEffectKey());
        form.setSoundEffectUrl(task.getSoundEffectUrl());
        form.setStrategyNote(task.getStrategyNote());
        form.setRelationshipStage(task.getRelationshipStage());
        form.setReason(task.getReason());
        form.setNote(task.getNote());
        form.setVisualPrompt(task.getVisualPrompt());
        form.setMotionPrompt(task.getMotionPrompt());
        form.setSessionId(task.getSessionId());
        form.setProvider(task.getProvider());
        form.setGenerateImage(true);
        form.setImageCount(1);
        return form;
    }

    private List<String> tryGenerateGiftImages(AgentGiftExecuteForm form, List<String> referenceMedia) {
        if (imageStrategyManager == null) {
            return Collections.emptyList();
        }
        if (referenceMedia == null || referenceMedia.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("function_type", "agent_gift_image");
        params.put("scene_code", "agent_gift_execute");
        params.put("size", "2K");
        params.put("response_format", "url");
        params.put("watermark", true);
        params.put("max_images", normalizeImageCount(form.getImageCount()));
        params.put("gift_code", limitText(form.getGiftCode(), 32));
        params.put("sequential_image_generation", "auto");
        List<String> generated = imageStrategyManager.generateImages(
                firstNonBlank(form.getVisualPrompt(), form.getNote(), form.getGiftName()),
                referenceMedia,
                params
        );
        if (generated == null || generated.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (String item : generated) {
            if (StringUtils.isNotBlank(item)) {
                urls.add(item.trim());
            }
        }
        return urls;
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
        if (!StringUtils.startsWithAny(normalizedUrl, "http://", "https://")) {
            return normalizedUrl;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .timeout(Duration.ofSeconds(45))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Referer", "https://image.baidu.com/")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = imageDownloadClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().length == 0) {
                throw new IllegalStateException("download failed: http " + response.statusCode());
            }
            String suffix = inferSuffix(normalizedUrl, response.headers().firstValue("Content-Type").orElse(""));
            String uploaded = OSSFactory.build().uploadSuffix(new ByteArrayInputStream(response.body()), suffix);
            if (StringUtils.isNotBlank(uploaded)) {
                return uploaded;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[gift-execute] persist generated image interrupted: {}", ex.getMessage());
        } catch (IOException | RuntimeException ex) {
            log.warn("[gift-execute] persist generated image failed, keep source url. url={}, reason={}", normalizedUrl, ex.getMessage());
        }
        return normalizedUrl;
    }

    private String inferSuffix(String imageUrl, String contentType) {
        String normalizedType = StringUtils.lowerCase(StringUtils.trimToEmpty(contentType));
        if (normalizedType.contains("png")) {
            return "png";
        }
        if (normalizedType.contains("webp")) {
            return "webp";
        }
        if (normalizedType.contains("gif")) {
            return "gif";
        }
        String lowerUrl = StringUtils.lowerCase(StringUtils.trimToEmpty(imageUrl));
        if (lowerUrl.contains(".png")) {
            return "png";
        }
        if (lowerUrl.contains(".webp")) {
            return "webp";
        }
        if (lowerUrl.contains(".gif")) {
            return "gif";
        }
        return "jpg";
    }

    private List<String> collectReferenceMedia(AppUserEntity sender, AppUserEntity target, int maxCount) {
        Set<String> urls = new LinkedHashSet<>();
        appendUserMedia(urls, sender, maxCount);
        appendUserMedia(urls, target, maxCount);
        return new ArrayList<>(urls);
    }

    private void appendUserMedia(Set<String> urls, AppUserEntity user, int maxCount) {
        if (user == null || urls.size() >= maxCount) {
            return;
        }
        if (isUsableImage(user.getAvatar())) {
            urls.add(user.getAvatar().trim());
        }
        if (urls.size() >= maxCount || StringUtils.isBlank(user.getFigur())) {
            return;
        }
        for (String item : parseLegacyImageList(user.getFigur())) {
            if (isUsableImage(item)) {
                urls.add(item.trim());
            }
            if (urls.size() >= maxCount) {
                break;
            }
        }
    }

    private List<String> parseLegacyImageList(String raw) {
        return parseStringList(raw);
    }

    private List<String> parseStringList(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("[")) {
                return JSON.parseArray(trimmed, String.class);
            }
        } catch (Exception ex) {
            log.warn("[gift-execute] parse figure json failed: {}", ex.getMessage());
        }
        String[] items = trimmed.split(",");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (StringUtils.isNotBlank(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private boolean isUsableImage(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("/");
    }

    private AppUserEntity requireUser(Integer uid, String message) {
        AppUserEntity user = appUserService.getById(uid);
        if (user == null) {
            throw new LinfengException(message);
        }
        return user;
    }

    private PostEntity resolveOptionalPost(Integer postId) {
        if (postId == null || postId <= 0) {
            return null;
        }
        PostEntity post = postService.getById(postId);
        if (post == null) {
            throw new LinfengException("关联帖子不存在");
        }
        return post;
    }

    private String resolveBillLinkId(GiftTaskEntity task, PostEntity post) {
        if (post != null && post.getId() != null) {
            return String.valueOf(post.getId());
        }
        if (task != null && task.getId() != null) {
            return "gift_task_" + task.getId();
        }
        return "gift_task";
    }

    private int normalizeImageCount(Integer imageCount) {
        if (imageCount == null) {
            return 1;
        }
        return Math.max(1, Math.min(3, imageCount));
    }

    public String toStatusText(Integer status) {
        if (status != null && status == TASK_STATUS_PROCESSING) {
            return "processing";
        }
        if (status == TASK_STATUS_SUCCESS) {
            return "success";
        }
        if (status == TASK_STATUS_FAILED) {
            return "failed";
        }
        return "pending";
    }

    public String toGiftStatusText(Integer status) {
        if (status != null && status == GIFT_STATUS_ACCEPTED) {
            return "accepted";
        }
        if (status != null && status == GIFT_STATUS_REJECTED) {
            return "rejected";
        }
        if (status != null && status == GIFT_STATUS_EXPIRED) {
            return "expired";
        }
        return "pending_accept";
    }

    public boolean isFinalStatus(Integer status) {
        return status != null && (status == TASK_STATUS_SUCCESS || status == TASK_STATUS_FAILED);
    }

    private GiftTaskEntity requireTask(String requestId, Integer requesterUid, Integer targetUid, String sessionId) {
        String safeRequestId = StringUtils.trimToEmpty(requestId);
        if (StringUtils.isBlank(safeRequestId)) {
            throw new LinfengException("礼物请求不能为空");
        }
        GiftTaskEntity task = giftTaskDao.selectOne(new LambdaQueryWrapper<GiftTaskEntity>()
                .eq(GiftTaskEntity::getRequestId, safeRequestId)
                .last("limit 1"));
        if (task == null) {
            throw new LinfengException("礼物请求不存在");
        }
        if (requesterUid != null && requesterUid > 0 && !Objects.equals(task.getSenderUid(), requesterUid)) {
            throw new LinfengException("礼物发送人校验失败");
        }
        if (targetUid != null && targetUid > 0 && !Objects.equals(task.getTargetUid(), targetUid)) {
            throw new LinfengException("当前用户无权处理该礼物");
        }
        if (StringUtils.isNotBlank(sessionId) && !StringUtils.equals(task.getSessionId(), sessionId)) {
            throw new LinfengException("会话信息不匹配");
        }
        return task;
    }

    private void sendGiftTipMessage(GiftTaskEntity task) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "gift_tip");
        payload.put("requestId", limitText(task.getRequestId(), 64));
        payload.put("requesterUid", task.getSenderUid());
        payload.put("targetUid", task.getTargetUid());
        payload.put("amount", task.getRewardAmount());
        payload.put("giftCode", limitText(task.getGiftCode(), 24));
        payload.put("giftName", limitText(firstNonBlank(task.getGiftName(), "心意"), 14));
        payload.put("giftIcon", limitText(firstNonBlank(task.getGiftIcon(), "🎁"), 4));
        if (StringUtils.isNotBlank(task.getGiftTheme())) {
            payload.put("giftTheme", limitText(task.getGiftTheme(), 12));
        }
        if (StringUtils.isNotBlank(task.getTemplateCode())) {
            payload.put("tpl", limitText(task.getTemplateCode(), 24));
        }
        if (StringUtils.isNotBlank(task.getAnimationPreset())) {
            payload.put("anim", limitText(task.getAnimationPreset(), 24));
        }
        if (StringUtils.isNotBlank(task.getRevealEffect())) {
            payload.put("reveal", limitText(task.getRevealEffect(), 24));
        }
        if (StringUtils.isNotBlank(task.getSoundEffectKey())) {
            payload.put("sfx", limitText(task.getSoundEffectKey(), 24));
        }
        if (StringUtils.isNotBlank(task.getGiftScene())) {
            payload.put("giftScene", limitText(task.getGiftScene(), 12));
        }
        if (StringUtils.isNotBlank(task.getReason())) {
            payload.put("reason", limitText(task.getReason(), 24));
        }
        if (StringUtils.isNotBlank(task.getNote())) {
            payload.put("note", limitText(task.getNote(), 36));
        }
        if (task.getId() != null && task.getId() > 0) {
            payload.put("taskId", task.getId());
        }
        payload.put("giftStatus", toGiftStatusText(task.getGiftStatus()));
        payload.put("taskStatus", toStatusText(task.getTaskStatus()));
        shrinkGiftTipPayload(payload);
        socialIntentMessageService.sendStructuredMessage(
                task.getSessionId(),
                task.getSenderUid(),
                task.getTargetUid(),
                payload
        );
    }

    private void shrinkGiftTipPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        String[] removableKeys = {"note", "reason", "giftScene", "giftIcon", "anim", "sfx", "giftCode"};
        for (String key : removableKeys) {
            if (buildSocialIntentMessage(payload).length() <= MAX_SOCIAL_INTENT_MESSAGE_LENGTH) {
                return;
            }
            payload.remove(key);
        }
        if (buildSocialIntentMessage(payload).length() <= MAX_SOCIAL_INTENT_MESSAGE_LENGTH) {
            return;
        }
        Object giftName = payload.get("giftName");
        if (giftName != null) {
            payload.put("giftName", limitText(String.valueOf(giftName), 8));
        }
    }

    private String buildSocialIntentMessage(Map<String, Object> payload) {
        return SocialIntentMessageService.SOCIAL_INTENT_PREFIX + JSON.toJSONString(payload);
    }

    private void syncGiftTipMessageState(GiftTaskEntity task) {
        if (task == null) {
            return;
        }
        socialIntentMessageService.syncGiftTipMessageState(
                task.getSessionId(),
                task.getRequestId(),
                toGiftStatusText(task.getGiftStatus()),
                toStatusText(task.getTaskStatus())
        );
    }

    private BigDecimal parseCommissionRate(String configValue) {
        if (StringUtils.isBlank(configValue)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal rate = new BigDecimal(configValue.trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (rate.compareTo(new BigDecimal("100")) > 0) {
                return new BigDecimal("100");
            }
            return rate;
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String buildFallbackVisualPrompt(AppUserEntity sender, AppUserEntity target, AgentGiftExecuteForm form) {
        String senderName = firstNonBlank(sender.getUsername(), "男生");
        String targetName = firstNonBlank(target.getUsername(), "女生");
        String city = firstNonBlank(target.getAbodeCity(), target.getLocationCity(), target.getCity(), sender.getCity(), "城市街景");
        String scene = firstNonBlank(form.getGiftScene(), "轻熟生活感约会场景");
        return senderName + "在" + city + "的真实生活场景里，把" + form.getGiftName() + "送给" + targetName
                + "，围绕" + scene + "展开，东亚年轻人，真人质感，电影感生活照。";
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

    private String limitText(String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
