package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 相亲局报名Service
 *
 * @author system
 * @date 2026-01-27
 */
public interface XiangqinEnrollmentService extends IService<XiangqinEnrollmentEntity> {

    /**
     * 分页查询报名列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 用户报名
     */
    void enroll(XiangqinEnrollmentEntity enrollment);

    /**
     * 取消报名
     */
    void cancelEnroll(Integer enrollmentId, Integer userId);

    /**
     * 审核报名
     */
    void auditEnroll(Integer enrollmentId, Integer status, String auditRemark);

    /**
     * 根据活动ID查询报名列表
     */
    List<XiangqinEnrollmentEntity> getByActivityId(Integer activityId);

    /**
     * 根据用户ID查询报名列表
     */
    List<XiangqinEnrollmentEntity> getByUserId(Integer userId);

    /**
     * 检查用户是否已报名
     */
    boolean checkEnrolled(Integer activityId, Integer userId);

    /**
     * 计算当前用户报名活动应支付金额
     */
    BigDecimal resolvePaymentAmount(Integer activityId, Integer userId);

    /**
     * 付费活动创建或复用待支付报名记录
     */
    XiangqinEnrollmentEntity createOrReusePendingPaidEnrollment(XiangqinEnrollmentEntity enrollment);

    /**
     * 支付成功后确认活动报名
     */
    void confirmPaidEnrollment(Integer enrollmentId, BigDecimal paymentAmount, Date paymentTime);
}
