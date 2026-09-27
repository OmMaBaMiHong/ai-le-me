package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.admin.dao.XiangqinActivityDao;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.ActivityChatGroupService;
import org.aileme.shejiao.api.service.ChatMessageService;
import org.aileme.shejiao.api.service.FriendService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.api.service.XiangqinEnrollmentService;
import org.aileme.shejiao.api.service.UserRechargeService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.app.websocket.component.SocketServer;
import org.aileme.shejiao.app.websocket.constant.MessageConstant;
import org.aileme.shejiao.app.websocket.entity.SocketMessage;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.AppletPayUtil;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.R;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;
import org.aileme.shejiao.domain.entity.app.ChatMessageEntity;
import org.aileme.shejiao.domain.param.app.ActivityEnrollPayForm;
import org.aileme.shejiao.domain.vo.OrderPay;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * App端-相亲局Controller
 */
@Slf4j
@RestController
@RequestMapping("/app/xiangqin")
@Tag(name = "App端——相亲局")
public class AppXiangqinController {

    @Autowired
    private XiangqinActivityService activityService;

    @Autowired
    private XiangqinEnrollmentService enrollmentService;

    @Autowired
    private XiangqinActivityDao activityDao;

    @Autowired
    private HongniangService hongniangService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private FriendService friendService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private MiniAppFilingFeatureService miniAppFilingFeatureService;

    @Autowired
    private ActivityChatGroupService activityChatGroupService;

    @Autowired
    private UserRechargeService userRechargeService;

    @GetMapping("/list")
    @Operation(summary = "相亲局列表")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = activityService.getAppActivityList(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "相亲局详情")
    public R info(@PathVariable("id") Integer id) {
        // 使用关联查询获取活动详情（包含红娘信息）
        XiangqinActivityEntity activity = activityDao.getActivityWithHongniang(id);
        if (activity != null) {
            // 增加浏览次数
            activityService.increaseViewCount(id);
        }
        return R.ok().put("activity", activity);
    }

    @Login
    @PostMapping("/enroll")
    @Operation(summary = "报名相亲局")
    public R enroll(@RequestBody XiangqinEnrollmentEntity enrollment,
                    @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireHongniangEnabled();
        enrollment.setUserId(user.getUid());
        if (enrollment.getRealName() == null || enrollment.getRealName().trim().isEmpty()) {
            enrollment.setRealName(user.getUsername());
        }
        XiangqinActivityEntity activity = activityDao.getActivityWithHongniang(enrollment.getActivityId());
        if (activity == null) {
            return R.error("活动不存在");
        }
        Integer organizerUid = resolveActivityOrganizerUid(activity);
        if (organizerUid != null && Objects.equals(organizerUid, user.getUid())) {
            return R.error("不能报名自己发起的活动");
        }
        enrollmentService.enroll(enrollment);
        sendEnrollmentChatNotice(activity, user, enrollment);
        return R.ok().put("msg", "报名成功，请等待审核");
    }

    @Login
    @PostMapping("/enrollPay")
    @Operation(summary = "活动报名并支付")
    public R enrollPay(@Valid @RequestBody ActivityEnrollPayForm form,
                       @Parameter(hidden = true) @LoginUser AppUserEntity user,
                       HttpServletRequest request) throws Exception {
        miniAppFilingFeatureService.requireHongniangEnabled();
        XiangqinActivityEntity activity = activityDao.getActivityWithHongniang(form.getActivityId());
        if (activity == null) {
            return R.error("活动不存在");
        }
        Integer organizerUid = resolveActivityOrganizerUid(activity);
        if (organizerUid != null && Objects.equals(organizerUid, user.getUid())) {
            return R.error("不能报名自己发起的活动");
        }

        XiangqinEnrollmentEntity enrollment = buildEnrollmentForm(form, user);
        BigDecimal paymentAmount = enrollmentService.resolvePaymentAmount(activity.getId(), user.getUid());
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            enrollmentService.enroll(enrollment);
            sendEnrollmentChatNotice(activity, user, enrollment);
            return R.ok()
                    .put("requiresPayment", false)
                    .put("msg", "报名成功，请等待审核");
        }

        String payType = normalizePayType(form.getPayType());
        String payChannel = normalizePayChannel(form.getPayChannel());
        XiangqinEnrollmentEntity pendingEnrollment = enrollmentService.createOrReusePendingPaidEnrollment(enrollment);
        String orderSn = userRechargeService.addActivityOrder(user, pendingEnrollment, activity, payType, payChannel);
        String ip = AppletPayUtil.getClientIp(request);
        OrderPay orderPay = userRechargeService.goToPay(orderSn, paymentAmount.doubleValue(), ip, user, payType,
                payChannel, resolveH5Origin(request));
        return R.ok()
                .put("requiresPayment", true)
                .put("orderId", orderSn)
                .put("enrollmentId", pendingEnrollment.getId())
                .put("amount", paymentAmount)
                .put("data", orderPay);
    }

    @PostMapping("/cancelEnroll")
    @Operation(summary = "取消报名")
    public R cancelEnroll(@RequestParam Integer enrollmentId, @RequestParam Integer userId) {
        enrollmentService.cancelEnroll(enrollmentId, userId);
        return R.ok().put("msg", "已取消报名");
    }

    @GetMapping("/myEnrollments")
    @Operation(summary = "我的报名")
    public R myEnrollments(@RequestParam Integer userId) {
        List<XiangqinEnrollmentEntity> list = enrollmentService.getByUserId(userId);
        return R.ok().put("list", list);
    }

    @Login
    @GetMapping("/myRelatedActivities")
    @Operation(summary = "我可关联发布的活动")
    public R myRelatedActivities(@Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer hongniangId = resolveHongniangId(user);

        Map<Integer, XiangqinActivityEntity> relatedMap = new LinkedHashMap<>();
        if (hongniangId != null && hongniangId > 0) {
            List<XiangqinActivityEntity> organizedList = activityService.getByHongniangId(hongniangId);
            organizedList.forEach(item -> relatedMap.put(item.getId(), item));
        }

        List<XiangqinEnrollmentEntity> enrollmentList = enrollmentService.getByUserId(user.getUid());
        for (XiangqinEnrollmentEntity enrollment : enrollmentList) {
            if (enrollment.getStatus() != null && enrollment.getStatus() == 3) {
                continue;
            }
            XiangqinActivityEntity activity = activityService.getById(enrollment.getActivityId());
            if (activity != null) {
                relatedMap.putIfAbsent(activity.getId(), activity);
            }
        }

        List<XiangqinActivityEntity> list = new ArrayList<>(relatedMap.values());
        list.sort(Comparator.comparing(
                XiangqinActivityEntity::getStartTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());
        return R.ok().put("list", list);
    }

    @Login
    @GetMapping("/myAuditList")
    @Operation(summary = "主理人报名审核列表")
    public R myAuditList(@RequestParam(required = false) Integer status,
                         @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer hongniangId = resolveHongniangId(user);
        if (hongniangId == null || hongniangId <= 0) {
            return R.ok().put("list", Collections.emptyList()).put("pendingCount", 0);
        }

        List<XiangqinActivityEntity> activityList = activityService.getByHongniangId(hongniangId);
        if (activityList == null || activityList.isEmpty()) {
            return R.ok().put("list", Collections.emptyList()).put("pendingCount", 0);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int pendingCount = 0;
        for (XiangqinActivityEntity activity : activityList) {
            if (activity == null || activity.getId() == null) {
                continue;
            }
            List<XiangqinEnrollmentEntity> enrollments = enrollmentService.getByActivityId(activity.getId());
            if (enrollments == null || enrollments.isEmpty()) {
                continue;
            }
            for (XiangqinEnrollmentEntity enrollment : enrollments) {
                if (enrollment == null || enrollment.getId() == null) {
                    continue;
                }
                if (enrollment.getUserId() != null && Objects.equals(enrollment.getUserId(), user.getUid())) {
                    continue;
                }
                Integer currentStatus = enrollment.getStatus();
                if (currentStatus != null && currentStatus == 3) {
                    continue;
                }
                if (status != null && !Objects.equals(currentStatus, status)) {
                    continue;
                }
                if (currentStatus != null && currentStatus == 0) {
                    pendingCount += 1;
                }
                result.add(buildAuditItem(activity, enrollment));
            }
        }

        result.sort(Comparator
                .comparing((Map<String, Object> item) -> Integer.parseInt(String.valueOf(item.getOrDefault("status", 0))))
                .thenComparing((Map<String, Object> item) -> String.valueOf(item.getOrDefault("createTime", "")), Comparator.reverseOrder()));
        return R.ok().put("list", result).put("pendingCount", pendingCount);
    }

    @Login
    @PostMapping("/auditEnroll")
    @Operation(summary = "主理人审核报名")
    public R auditEnroll(@RequestBody Map<String, Object> params,
                         @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (params == null || params.get("enrollmentId") == null || params.get("status") == null) {
            return R.error("审核参数不完整");
        }
        Integer enrollmentId = Integer.valueOf(String.valueOf(params.get("enrollmentId")));
        Integer status = Integer.valueOf(String.valueOf(params.get("status")));
        String auditRemark = params.get("auditRemark") == null ? "" : String.valueOf(params.get("auditRemark"));
        if (!Objects.equals(status, 1) && !Objects.equals(status, 2)) {
            return R.error("审核状态无效");
        }

        try {
            auditEnrollmentInternal(enrollmentId, status, auditRemark, user, true);
            return R.ok().put("msg", status == 1 ? "已通过报名" : "已拒绝报名");
        } catch (LinfengException ex) {
            return R.error(ex.getMessage());
        }
    }

    @Login
    @PostMapping("/auditEnrollBatch")
    @Operation(summary = "主理人批量审核报名")
    public R auditEnrollBatch(@RequestBody Map<String, Object> params,
                              @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        if (params == null || params.get("enrollmentIds") == null || params.get("status") == null) {
            return R.error("批量审核参数不完整");
        }
        Integer status = Integer.valueOf(String.valueOf(params.get("status")));
        if (!Objects.equals(status, 1) && !Objects.equals(status, 2)) {
            return R.error("审核状态无效");
        }
        String auditRemark = params.get("auditRemark") == null ? "" : String.valueOf(params.get("auditRemark"));
        Object enrollmentIdsRaw = params.get("enrollmentIds");
        if (!(enrollmentIdsRaw instanceof List<?> rawList) || rawList.isEmpty()) {
            return R.error("请选择至少一条报名记录");
        }

        Set<Integer> enrollmentIds = new LinkedHashSet<>();
        for (Object rawId : rawList) {
            if (rawId == null) {
                continue;
            }
            enrollmentIds.add(Integer.valueOf(String.valueOf(rawId)));
        }
        if (enrollmentIds.isEmpty()) {
            return R.error("请选择至少一条报名记录");
        }

        List<Map<String, Object>> failedList = new ArrayList<>();
        int successCount = 0;
        for (Integer enrollmentId : enrollmentIds) {
            try {
                auditEnrollmentInternal(enrollmentId, status, auditRemark, user, true);
                successCount += 1;
            } catch (Exception ex) {
                Map<String, Object> failed = new LinkedHashMap<>();
                failed.put("enrollmentId", enrollmentId);
                failed.put("reason", ex.getMessage());
                failedList.add(failed);
            }
        }

        if (successCount <= 0) {
            return R.error(failedList.isEmpty() ? "批量审核失败" : String.valueOf(failedList.get(0).get("reason")))
                    .put("successCount", 0)
                    .put("failedCount", failedList.size())
                    .put("failedList", failedList);
        }

        String msg = status == 1
                ? String.format("已通过 %d 条报名", successCount)
                : String.format("已拒绝 %d 条报名", successCount);
        if (!failedList.isEmpty()) {
            msg = msg + String.format("，%d 条处理失败", failedList.size());
        }
        return R.ok()
                .put("msg", msg)
                .put("successCount", successCount)
                .put("failedCount", failedList.size())
                .put("failedList", failedList);
    }

    @GetMapping("/enrollList")
    @Operation(summary = "活动报名列表")
    public R enrollList(@RequestParam Integer activityId) {
        List<XiangqinEnrollmentEntity> list = enrollmentService.getByActivityId(activityId);
        return R.ok().put("list", list);
    }

    @GetMapping("/checkEnrolled")
    @Operation(summary = "检查是否已报名")
    public R checkEnrolled(@RequestParam Integer activityId, @RequestParam Integer userId) {
        boolean enrolled = enrollmentService.checkEnrolled(activityId, userId);
        return R.ok().put("enrolled", enrolled);
    }

    @Login
    @PostMapping("/save")
    @Operation(summary = "主理人创建相亲局")
    public R save(@RequestBody XiangqinActivityEntity activity,
                  @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer hongniangId = resolveHongniangId(user);
        if (hongniangId == null || hongniangId <= 0) {
            return R.error("当前账号还不是主理人");
        }

        activity.setHongniangId(hongniangId);
        if (activity.getStatus() == null) {
            activity.setStatus(1);
        }
        if (activity.getEnrollStartTime() == null) {
            activity.setEnrollStartTime(new Date());
        }
        if (activity.getEnrollEndTime() == null) {
            activity.setEnrollEndTime(activity.getStartTime());
        }

        activityService.saveActivity(activity);
        return R.ok().put("msg", "活动创建成功").put("activityId", activity.getId());
    }

    private Integer resolveHongniangId(AppUserEntity user) {
        Integer hongniangId = user.getHongniangId();
        if (hongniangId == null || hongniangId <= 0) {
            HongniangInfoEntity hongniang = hongniangService.getByUserId(user.getUid());
            if (hongniang != null) {
                hongniangId = hongniang.getId();
            }
        }
        return hongniangId;
    }

    private XiangqinEnrollmentEntity buildEnrollmentForm(ActivityEnrollPayForm form, AppUserEntity user) {
        XiangqinEnrollmentEntity enrollment = new XiangqinEnrollmentEntity();
        enrollment.setActivityId(form.getActivityId());
        enrollment.setUserId(user.getUid());
        enrollment.setRealName(form.getRealName() == null || form.getRealName().trim().isEmpty()
                ? user.getUsername()
                : form.getRealName().trim());
        enrollment.setPhone(form.getPhone());
        enrollment.setWechat(form.getWechat());
        enrollment.setEnrollRemark(form.getEnrollRemark());
        return enrollment;
    }

    private String normalizePayType(String payType) {
        if (payType == null || payType.trim().isEmpty()) {
            return "h5";
        }
        return payType.trim();
    }

    private String normalizePayChannel(String payChannel) {
        if ("alipay".equalsIgnoreCase(payChannel)) {
            return "alipay";
        }
        return "wechat";
    }

    private String resolveH5Origin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.trim().isEmpty()) {
            return origin.trim();
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            int slashIndex = referer.indexOf('/', referer.indexOf("://") + 3);
            return slashIndex > 0 ? referer.substring(0, slashIndex) : referer;
        }
        return request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
    }

    private Integer resolveActivityOrganizerUid(XiangqinActivityEntity activity) {
        if (activity == null || activity.getHongniangId() == null || activity.getHongniangId() <= 0) {
            return null;
        }
        HongniangInfoEntity hongniang = hongniangService.getById(activity.getHongniangId());
        return hongniang == null ? null : hongniang.getUserId();
    }

    private void auditEnrollmentInternal(Integer enrollmentId, Integer status, String auditRemark, AppUserEntity user, boolean sendNotice) {
        XiangqinEnrollmentEntity enrollment = enrollmentService.getById(enrollmentId);
        if (enrollment == null) {
            throw new LinfengException("报名记录不存在");
        }
        XiangqinActivityEntity activity = activityService.getById(enrollment.getActivityId());
        if (activity == null) {
            throw new LinfengException("活动不存在");
        }
        if (enrollment.getUserId() != null && Objects.equals(enrollment.getUserId(), user.getUid())) {
            throw new LinfengException("不能审核自己提交的报名");
        }
        if (enrollment.getStatus() != null && !Objects.equals(enrollment.getStatus(), 0)) {
            throw new LinfengException("该报名已处理");
        }
        Integer hongniangId = resolveHongniangId(user);
        if (hongniangId == null || !hongniangId.equals(activity.getHongniangId())) {
            throw new LinfengException("无权审核该报名");
        }

        enrollmentService.auditEnroll(enrollmentId, status, auditRemark);
        if (Objects.equals(status, 1)) {
            activityChatGroupService.syncApprovedMembers(activity.getId());
        }
        if (!sendNotice) {
            return;
        }
        try {
            sendAuditResultChatNotice(activity, enrollment, status, auditRemark);
        } catch (Exception ex) {
            log.error("发送活动审核通知失败 enrollmentId={}, status={}, error={}", enrollmentId, status, ex.getMessage(), ex);
        }
    }

    private Map<String, Object> buildAuditItem(XiangqinActivityEntity activity, XiangqinEnrollmentEntity enrollment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", enrollment.getId());
        item.put("activityId", activity.getId());
        item.put("activityTitle", activity.getTitle());
        item.put("activityCover", activity.getCoverImg());
        item.put("activityStartTime", activity.getStartTime());
        item.put("userId", enrollment.getUserId());
        item.put("realName", enrollment.getRealName());
        item.put("phone", enrollment.getPhone());
        item.put("wechat", enrollment.getWechat());
        item.put("enrollRemark", enrollment.getEnrollRemark());
        item.put("status", enrollment.getStatus());
        item.put("auditRemark", enrollment.getAuditRemark());
        item.put("createTime", enrollment.getCreateTime());
        AppUserEntity applicant = enrollment.getUserId() == null ? null : appUserService.getById(enrollment.getUserId());
        if (applicant != null) {
            item.put("userAvatar", applicant.getAvatar());
            item.put("username", applicant.getUsername());
            item.put("gender", applicant.getGender());
            item.put("age", applicant.getAge());
        }
        return item;
    }

    private void sendEnrollmentChatNotice(XiangqinActivityEntity activity, AppUserEntity applicant, XiangqinEnrollmentEntity enrollment) {
        if (activity == null || applicant == null) {
            return;
        }
        HongniangInfoEntity hongniang = activity.getHongniangId() == null ? null : hongniangService.getById(activity.getHongniangId());
        Integer organizerUid = hongniang == null ? null : hongniang.getUserId();
        if (organizerUid == null || organizerUid <= 0 || organizerUid.equals(applicant.getUid())) {
            return;
        }

        String applicantName = applicant.getUsername() == null || applicant.getUsername().trim().isEmpty()
                ? "有用户"
                : applicant.getUsername();
        String content = String.format("%s 报名了你的活动《%s》，请及时审核。", applicantName, activity.getTitle());
        if (enrollment.getPhone() != null && !enrollment.getPhone().trim().isEmpty()) {
            content = content + " 联系电话：" + enrollment.getPhone();
        }
        sendChatTextNotice(applicant.getUid(), organizerUid, content);
    }

    private void sendAuditResultChatNotice(XiangqinActivityEntity activity, XiangqinEnrollmentEntity enrollment, Integer status, String auditRemark) {
        if (activity == null || enrollment == null || enrollment.getUserId() == null || enrollment.getUserId() <= 0) {
            return;
        }
        HongniangInfoEntity hongniang = activity.getHongniangId() == null ? null : hongniangService.getById(activity.getHongniangId());
        Integer organizerUid = hongniang == null ? null : hongniang.getUserId();
        if (organizerUid == null || organizerUid <= 0) {
            return;
        }
        String content = status != null && status == 1
                ? String.format("你报名的活动《%s》已通过审核。", activity.getTitle())
                : String.format("你报名的活动《%s》未通过审核。", activity.getTitle());
        if (auditRemark != null && !auditRemark.trim().isEmpty()) {
            content = content + " 备注：" + auditRemark.trim();
        }
        sendChatTextNotice(organizerUid, enrollment.getUserId(), content);
    }

    private void sendChatTextNotice(Integer senderUid, Integer receiverUid, String content) {
        if (senderUid == null || receiverUid == null || senderUid <= 0 || receiverUid <= 0 || senderUid.equals(receiverUid)) {
            return;
        }
        Long sessionId = friendService.getOrCreateSession(senderUid, receiverUid);
        ChatMessageEntity chatMessage = ChatMessageEntity.builder()
                .sessionId(String.valueOf(sessionId))
                .senderId(String.valueOf(senderUid))
                .receiverId(String.valueOf(receiverUid))
                .sendTime(DateUtil.nowDateTimeStr())
                .content(content)
                .messageType("text")
                .isWithdrawn(0)
                .updateTime(DateUtil.nowDateTime())
                .build();
        chatMessageService.saveMessage(chatMessage);
        SocketMessage<ChatMessageEntity> socketMessage = new SocketMessage<>(MessageConstant.PERSON_MESSAGE, chatMessage);
        SocketServer.sendToUser(String.valueOf(receiverUid), socketMessage);
        SocketServer.sendToUser(String.valueOf(senderUid), socketMessage);
    }
}
