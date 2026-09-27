package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.XiangqinActivityDao;
import org.aileme.shejiao.admin.dao.XiangqinEnrollmentDao;
import org.aileme.shejiao.api.service.XiangqinActivityService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 相亲局活动Service实现
 *
 * @author system
 * @date 2026-01-27
 */
@DS("master")
@Service("xiangqinActivityService")
public class XiangqinActivityServiceImpl extends ServiceImpl<XiangqinActivityDao, XiangqinActivityEntity> implements XiangqinActivityService {

    @Autowired
    private XiangqinActivityDao activityDao;

    @Autowired
    private XiangqinEnrollmentDao enrollmentDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String title = (String) params.get("title");
        Integer hongniangId = params.get("hongniangId") != null ? Integer.valueOf(params.get("hongniangId").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;

        QueryWrapper<XiangqinActivityEntity> wrapper = new QueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like("title", title);
        }
        if (hongniangId != null) {
            wrapper.eq("hongniang_id", hongniangId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        IPage<XiangqinActivityEntity> page = this.page(new Query<XiangqinActivityEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    @DSTransactional
    public void saveActivity(XiangqinActivityEntity activity) {
        // 初始化数据
        activity.setMaleCount(0);
        activity.setFemaleCount(0);
        activity.setViewCount(0);
        activity.setCreateTime(new Date());
        activity.setUpdateTime(new Date());

        // 验证时间
        if (activity.getStartTime() != null && activity.getEndTime() != null) {
            if (activity.getStartTime().after(activity.getEndTime())) {
                throw new LinfengException("活动开始时间不能晚于结束时间");
            }
        }

        this.save(activity);
    }

    @Override
    @DSTransactional
    public void updateActivity(XiangqinActivityEntity activity) {
        activity.setUpdateTime(new Date());
        this.updateById(activity);
    }

    @Override
    public List<XiangqinActivityEntity> getByHongniangId(Integer hongniangId) {
        return activityDao.getByHongniangId(hongniangId);
    }

    @Override
    @DSTransactional
    public void increaseViewCount(Integer activityId) {
        activityDao.increaseViewCount(activityId);
    }

    @Override
    @DSTransactional
    public void updateEnrollCount(Integer activityId) {
        List<XiangqinEnrollmentEntity> enrollmentList = enrollmentDao.getByActivityId(activityId);
        int maleCount = 0;
        int femaleCount = 0;
        for (XiangqinEnrollmentEntity item : enrollmentList) {
            if (item == null || item.getStatus() == null || item.getStatus() != 1) {
                continue;
            }
            Integer gender = item.getGender() == null ? 0 : item.getGender();
            if (gender == 1) {
                maleCount++;
            } else if (gender == 2) {
                femaleCount++;
            }
        }

        activityDao.updateEnrollCount(activityId, maleCount, femaleCount);
    }

    @Override
    public PageUtils getAppActivityList(Map<String, Object> params) {
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : 1; // 默认报名中
        Integer hongniangId = params.get("hongniangId") != null ? Integer.valueOf(params.get("hongniangId").toString()) : null;
        Integer activityType = params.get("activityType") != null ? Integer.valueOf(params.get("activityType").toString()) : null;
        long currPage = params.get("page") != null ? Long.parseLong(String.valueOf(params.get("page"))) : 1L;
        long limit = params.get("limit") != null ? Long.parseLong(String.valueOf(params.get("limit"))) : 10L;

        QueryWrapper<XiangqinActivityEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        if (hongniangId != null) {
            wrapper.eq("hongniang_id", hongniangId);
        }
        if (activityType != null) {
            wrapper.eq("activity_type", activityType);
        }
        int totalCount = Math.toIntExact(this.count(wrapper));
        long offset = Math.max(currPage - 1, 0L) * limit;
        List<XiangqinActivityEntity> list = activityDao.getAppActivityList(status, hongniangId, activityType, offset, limit);
        return new PageUtils(list, totalCount, Math.toIntExact(limit), Math.toIntExact(currPage));
    }
}
