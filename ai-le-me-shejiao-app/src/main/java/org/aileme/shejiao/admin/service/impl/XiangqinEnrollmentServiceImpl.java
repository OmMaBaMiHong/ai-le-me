package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.XiangqinEnrollmentDao;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.api.service.XiangqinEnrollmentService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 相亲局报名Service实现
 *
 * @author system
 * @date 2026-01-27
 */
@DS("master")
@Service("xiangqinEnrollmentService")
public class XiangqinEnrollmentServiceImpl extends ServiceImpl<XiangqinEnrollmentDao, XiangqinEnrollmentEntity> implements XiangqinEnrollmentService {

    @Autowired
    private XiangqinEnrollmentDao enrollmentDao;

    @Autowired
    private XiangqinActivityService activityService;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private HongniangService hongniangService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Integer activityId = params.get("activityId") != null ? Integer.valueOf(params.get("activityId").toString()) : null;
        Integer userId = params.get("userId") != null ? Integer.valueOf(params.get("userId").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;

        long currPage = params.get("pageNum") != null ? Long.parseLong(String.valueOf(params.get("pageNum"))) :
            (params.get("page") != null ? Long.parseLong(String.valueOf(params.get("page"))) : 1L);
        long limit = params.get("pageSize") != null ? Long.parseLong(String.valueOf(params.get("pageSize"))) :
            (params.get("limit") != null ? Long.parseLong(String.valueOf(params.get("limit"))) : 10L);

        if (activityId != null) {
            List<XiangqinEnrollmentEntity> records = enrollmentDao.getByActivityId(activityId);
            if (status != null) {
                records = records.stream()
                    .filter(item -> status.equals(item.getStatus()))
                    .collect(Collectors.toList());
            }
            int totalCount = records.size();
            int fromIndex = (int) Math.min(Math.max(currPage - 1, 0L) * limit, totalCount);
            int toIndex = (int) Math.min(fromIndex + limit, totalCount);
            List<XiangqinEnrollmentEntity> pagedRecords = fromIndex >= toIndex
                ? Collections.emptyList()
                : records.subList(fromIndex, toIndex);
            return new PageUtils(pagedRecords, totalCount, Math.toIntExact(limit), Math.toIntExact(currPage));
        }

        QueryWrapper<XiangqinEnrollmentEntity> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        IPage<XiangqinEnrollmentEntity> page = this.page(new Query<XiangqinEnrollmentEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public void enroll(XiangqinEnrollmentEntity enrollment) {
        AppUserEntity user = getRequiredUser(enrollment.getUserId());
        XiangqinActivityEntity activity = getRequiredActivity(enrollment.getActivityId());
        BigDecimal paymentAmount = resolvePaymentAmount(activity.getId(), user.getUid());
        if (paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new LinfengException("收费活动请先支付后报名");
        }
        validateEnrollment(activity, user, null);

        enrollment.setStatus(0); // 待审核
        enrollment.setPaymentStatus(0); // 未支付
        enrollment.setPaymentAmount(BigDecimal.ZERO);
        enrollment.setPaymentTime(null);
        enrollment.setCheckInStatus(0); // 未签到
        enrollment.setCreateTime(new Date());
        enrollment.setUpdateTime(new Date());
        this.save(enrollment);
    }

    @Override
    @DSTransactional
    public XiangqinEnrollmentEntity createOrReusePendingPaidEnrollment(XiangqinEnrollmentEntity enrollment) {
        AppUserEntity user = getRequiredUser(enrollment.getUserId());
        XiangqinActivityEntity activity = getRequiredActivity(enrollment.getActivityId());
        BigDecimal paymentAmount = resolvePaymentAmount(activity.getId(), user.getUid());
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LinfengException("当前活动无需支付报名");
        }

        XiangqinEnrollmentEntity pendingEnrollment = findPendingPaidEnrollment(activity.getId(), user.getUid());
        validateEnrollment(activity, user, pendingEnrollment == null ? null : pendingEnrollment.getId());

        Date now = new Date();
        if (pendingEnrollment != null) {
            pendingEnrollment.setRealName(enrollment.getRealName());
            pendingEnrollment.setPhone(enrollment.getPhone());
            pendingEnrollment.setWechat(enrollment.getWechat());
            pendingEnrollment.setEnrollRemark(enrollment.getEnrollRemark());
            pendingEnrollment.setPaymentAmount(paymentAmount);
            pendingEnrollment.setUpdateTime(now);
            this.updateById(pendingEnrollment);
            return pendingEnrollment;
        }

        enrollment.setStatus(0);
        enrollment.setPaymentStatus(0);
        enrollment.setPaymentAmount(paymentAmount);
        enrollment.setPaymentTime(null);
        enrollment.setCheckInStatus(0);
        enrollment.setCreateTime(now);
        enrollment.setUpdateTime(now);
        this.save(enrollment);
        return enrollment;
    }

    @Override
    @DSTransactional
    public void cancelEnroll(Integer enrollmentId, Integer userId) {
        XiangqinEnrollmentEntity enrollment = this.getById(enrollmentId);
        if (enrollment == null) {
            throw new LinfengException("报名记录不存在");
        }
        if (!enrollment.getUserId().equals(userId)) {
            throw new LinfengException("无权取消该报名");
        }
        if (enrollment.getStatus() == 3) {
            throw new LinfengException("报名已取消");
        }

        enrollment.setStatus(3); // 已取消
        enrollment.setUpdateTime(new Date());
        this.updateById(enrollment);
    }

    @Override
    @DSTransactional
    public void auditEnroll(Integer enrollmentId, Integer status, String auditRemark) {
        XiangqinEnrollmentEntity enrollment = this.getById(enrollmentId);
        if (enrollment == null) {
            throw new LinfengException("报名记录不存在");
        }
        if (requiresPayment(enrollment) && !Objects.equals(enrollment.getPaymentStatus(), 1)) {
            throw new LinfengException("该用户尚未完成报名支付");
        }

        enrollment.setStatus(status);
        enrollment.setAuditRemark(auditRemark);
        enrollment.setUpdateTime(new Date());
        this.updateById(enrollment);

        // 如果通过审核，更新活动报名人数
        if (status == 1) {
            activityService.updateEnrollCount(enrollment.getActivityId());
        }
    }

    @Override
    public List<XiangqinEnrollmentEntity> getByActivityId(Integer activityId) {
        return enrollmentDao.getByActivityId(activityId);
    }

    @Override
    public List<XiangqinEnrollmentEntity> getByUserId(Integer userId) {
        return enrollmentDao.getByUserId(userId);
    }

    @Override
    public boolean checkEnrolled(Integer activityId, Integer userId) {
        if (activityId == null || userId == null) {
            return false;
        }
        return getByActivityId(activityId).stream()
                .anyMatch(item -> item != null && Objects.equals(item.getUserId(), userId) && isEffectiveEnrollment(item));
    }

    @Override
    public BigDecimal resolvePaymentAmount(Integer activityId, Integer userId) {
        XiangqinActivityEntity activity = getRequiredActivity(activityId);
        AppUserEntity user = getRequiredUser(userId);
        Integer feeType = activity.getFeeType() == null ? 0 : activity.getFeeType();
        Integer gender = user.getGender() == null ? 0 : user.getGender();

        if (feeType == 0) {
            return BigDecimal.ZERO;
        }
        if (feeType == 1) {
            if (gender == 0) {
                throw new LinfengException("请先完善性别信息后再报名该活动");
            }
            return gender == 1 ? normalizeAmount(activity.getMaleFee()) : BigDecimal.ZERO;
        }
        if (feeType == 2) {
            if (gender == 0) {
                throw new LinfengException("请先完善性别信息后再报名该活动");
            }
            return gender == 2 ? normalizeAmount(activity.getFemaleFee()) : BigDecimal.ZERO;
        }
        if (gender == 1) {
            return normalizeAmount(activity.getMaleFee());
        }
        if (gender == 2) {
            return normalizeAmount(activity.getFemaleFee());
        }
        throw new LinfengException("请先完善性别信息后再报名该活动");
    }

    @Override
    @DSTransactional
    public void confirmPaidEnrollment(Integer enrollmentId, BigDecimal paymentAmount, Date paymentTime) {
        XiangqinEnrollmentEntity enrollment = this.getById(enrollmentId);
        if (enrollment == null) {
            throw new LinfengException("报名记录不存在");
        }
        if (Objects.equals(enrollment.getStatus(), 3)) {
            throw new LinfengException("报名记录已取消");
        }

        XiangqinActivityEntity activity = getRequiredActivity(enrollment.getActivityId());
        AppUserEntity user = getRequiredUser(enrollment.getUserId());
        validateEnrollment(activity, user, enrollment.getId());

        enrollment.setPaymentStatus(1);
        enrollment.setPaymentAmount(normalizeAmount(paymentAmount).compareTo(BigDecimal.ZERO) > 0
                ? normalizeAmount(paymentAmount)
                : normalizeAmount(enrollment.getPaymentAmount()));
        enrollment.setPaymentTime(paymentTime == null ? new Date() : paymentTime);
        enrollment.setUpdateTime(new Date());
        this.updateById(enrollment);
    }

    private void validateEnrollment(XiangqinActivityEntity activity, AppUserEntity user, Integer currentEnrollmentId) {
        if (activity == null) {
            throw new LinfengException("活动不存在");
        }
        if (user == null) {
            throw new LinfengException("用户不存在");
        }
        if (!Objects.equals(activity.getStatus(), 1)) {
            throw new LinfengException("活动未开放报名");
        }

        Integer organizerUid = resolveOrganizerUid(activity);
        if (organizerUid != null && Objects.equals(organizerUid, user.getUid())) {
            throw new LinfengException("不能报名自己发起的活动");
        }

        Date now = new Date();
        if (activity.getEnrollStartTime() != null && now.before(activity.getEnrollStartTime())) {
            throw new LinfengException("报名暂未开始");
        }
        if (activity.getEnrollEndTime() != null && now.after(activity.getEnrollEndTime())) {
            throw new LinfengException("报名已截止");
        }
        if (activity.getStartTime() != null && !now.before(activity.getStartTime())) {
            throw new LinfengException("活动已开始，不能再报名");
        }
        if (activity.getEndTime() != null && !now.before(activity.getEndTime())) {
            throw new LinfengException("活动已结束");
        }

        Integer age = user.getAge() == null ? 0 : user.getAge();
        if ((activity.getAgeMin() != null || activity.getAgeMax() != null) && age <= 0) {
            throw new LinfengException("请先完善年龄信息后再报名");
        }
        if (activity.getAgeMin() != null && age < activity.getAgeMin()) {
            throw new LinfengException("未达到活动年龄要求");
        }
        if (activity.getAgeMax() != null && age > activity.getAgeMax()) {
            throw new LinfengException("已超过活动年龄要求");
        }

        List<XiangqinEnrollmentEntity> userEnrollments = this.lambdaQuery()
                .eq(XiangqinEnrollmentEntity::getActivityId, activity.getId())
                .eq(XiangqinEnrollmentEntity::getUserId, user.getUid())
                .ne(XiangqinEnrollmentEntity::getStatus, 3)
                .list();
        for (XiangqinEnrollmentEntity item : userEnrollments) {
            if (currentEnrollmentId != null && Objects.equals(item.getId(), currentEnrollmentId)) {
                continue;
            }
            if (isEffectiveEnrollment(item)) {
                throw new LinfengException("您已报名该活动");
            }
            if (isPendingPaidEnrollment(item)) {
                throw new LinfengException("您有一笔待支付报名，请先完成支付");
            }
        }

        Integer maxParticipants = activity.getMaxParticipants() == null ? 0 : activity.getMaxParticipants();
        if (maxParticipants > 0) {
            int effectiveCount = countEffectiveEnrollments(activity.getId(), currentEnrollmentId);
            if (effectiveCount >= maxParticipants) {
                throw new LinfengException("活动报名人数已满");
            }
        }
    }

    private int countEffectiveEnrollments(Integer activityId, Integer excludeEnrollmentId) {
        List<XiangqinEnrollmentEntity> enrollmentList = this.lambdaQuery()
                .eq(XiangqinEnrollmentEntity::getActivityId, activityId)
                .in(XiangqinEnrollmentEntity::getStatus, 0, 1)
                .list();
        int count = 0;
        for (XiangqinEnrollmentEntity item : enrollmentList) {
            if (excludeEnrollmentId != null && Objects.equals(item.getId(), excludeEnrollmentId)) {
                continue;
            }
            if (isEffectiveEnrollment(item)) {
                count++;
            }
        }
        return count;
    }

    private XiangqinEnrollmentEntity findPendingPaidEnrollment(Integer activityId, Integer userId) {
        List<XiangqinEnrollmentEntity> enrollmentList = this.lambdaQuery()
                .eq(XiangqinEnrollmentEntity::getActivityId, activityId)
                .eq(XiangqinEnrollmentEntity::getUserId, userId)
                .ne(XiangqinEnrollmentEntity::getStatus, 3)
                .orderByDesc(XiangqinEnrollmentEntity::getId)
                .list();
        for (XiangqinEnrollmentEntity item : enrollmentList) {
            if (isPendingPaidEnrollment(item)) {
                return item;
            }
        }
        return null;
    }

    private boolean isPendingPaidEnrollment(XiangqinEnrollmentEntity enrollment) {
        return enrollment != null
                && enrollment.getStatus() != null
                && enrollment.getStatus() != 3
                && requiresPayment(enrollment)
                && !Objects.equals(enrollment.getPaymentStatus(), 1);
    }

    private boolean isEffectiveEnrollment(XiangqinEnrollmentEntity enrollment) {
        return enrollment != null
                && enrollment.getStatus() != null
                && enrollment.getStatus() != 3
                && (!requiresPayment(enrollment) || Objects.equals(enrollment.getPaymentStatus(), 1));
    }

    private boolean requiresPayment(XiangqinEnrollmentEntity enrollment) {
        return normalizeAmount(enrollment == null ? null : enrollment.getPaymentAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private AppUserEntity getRequiredUser(Integer userId) {
        AppUserEntity user = appUserService.getById(userId);
        if (user == null) {
            throw new LinfengException("用户不存在");
        }
        return user;
    }

    private XiangqinActivityEntity getRequiredActivity(Integer activityId) {
        XiangqinActivityEntity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new LinfengException("活动不存在");
        }
        return activity;
    }

    private Integer resolveOrganizerUid(XiangqinActivityEntity activity) {
        if (activity == null || activity.getHongniangId() == null || activity.getHongniangId() <= 0) {
            return null;
        }
        HongniangInfoEntity hongniang = hongniangService.getById(activity.getHongniangId());
        return hongniang == null ? null : hongniang.getUserId();
    }
}
