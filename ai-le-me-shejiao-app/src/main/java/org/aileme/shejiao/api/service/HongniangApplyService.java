package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.HongniangApplyEntity;

import java.util.Map;

/**
 * 红娘申请Service
 *
 * @author system
 * @date 2026-02-10
 */
public interface HongniangApplyService extends IService<HongniangApplyEntity> {

    /**
     * 分页查询申请列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 用户提交申请
     */
    void submitApply(HongniangApplyEntity apply);

    /**
     * 审核申请
     */
    void auditApply(Integer applyId, Integer status, String auditRemark);

    /**
     * 根据用户ID查询最新申请状态
     */
    HongniangApplyEntity getLatestByUserId(Integer userId);
}
