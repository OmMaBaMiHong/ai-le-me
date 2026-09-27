package org.aileme.shejiao.admin.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.HongniangApplyDao;
import org.aileme.shejiao.api.service.HongniangApplyService;
import org.aileme.shejiao.api.service.HongniangService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.HongniangApplyEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;

import java.util.Date;
import java.util.Map;

/**
 * 红娘申请Service实现
 *
 * @author system
 * @date 2026-02-10
 */
@DS("master")
@Service("hongniangApplyService")
public class HongniangApplyServiceImpl extends ServiceImpl<HongniangApplyDao, HongniangApplyEntity> implements HongniangApplyService {

    @Autowired
    private HongniangApplyDao applyDao;

    @Autowired
    private HongniangService hongniangService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Integer userId = params.get("userId") != null ? Integer.valueOf(params.get("userId").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;

        QueryWrapper<HongniangApplyEntity> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        IPage<HongniangApplyEntity> page = this.page(new Query<HongniangApplyEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public void submitApply(HongniangApplyEntity apply) {
        // 检查是否已有待审核的申请
        HongniangApplyEntity existApply = applyDao.getLatestByUserId(apply.getUserId());
        if (existApply != null && existApply.getStatus() == 0) {
            throw new LinfengException("您已有待审核的申请，请耐心等待");
        }

        // 检查是否已经是红娘
        QueryWrapper<HongniangInfoEntity> hongniangWrapper = new QueryWrapper<>();
        hongniangWrapper.eq("user_id", apply.getUserId());
        HongniangInfoEntity hongniang = hongniangService.getOne(hongniangWrapper);
        if (hongniang != null) {
            throw new LinfengException("您已经是红娘了");
        }

        apply.setStatus(0); // 待审核
        apply.setCreateTime(new Date());
        apply.setUpdateTime(new Date());
        this.save(apply);
    }

    @Override
    @DSTransactional
    public void auditApply(Integer applyId, Integer status, String auditRemark) {
        HongniangApplyEntity apply = this.getById(applyId);
        if (apply == null) {
            throw new LinfengException("申请记录不存在");
        }
        if (apply.getStatus() != 0) {
            throw new LinfengException("该申请已审核过");
        }

        apply.setStatus(status);
        apply.setAuditRemark(auditRemark);
        apply.setAuditTime(new Date());
        apply.setUpdateTime(new Date());
        this.updateById(apply);

        // 如果审核通过，自动创建红娘信息
        if (status == 1) {
            HongniangInfoEntity hongniang = new HongniangInfoEntity();
            hongniang.setUserId(apply.getUserId());
            hongniang.setHongniangName(apply.getRealName());
            hongniang.setPhone(apply.getPhone());
            hongniang.setWechat(apply.getWechat());
            hongniang.setLevel(1); // 默认等级1
            hongniang.setStatus(1); // 启用
            hongniang.setCreateTime(new Date());
            hongniangService.save(hongniang);
        }
    }

    @Override
    public HongniangApplyEntity getLatestByUserId(Integer userId) {
        return applyDao.getLatestByUserId(userId);
    }
}
