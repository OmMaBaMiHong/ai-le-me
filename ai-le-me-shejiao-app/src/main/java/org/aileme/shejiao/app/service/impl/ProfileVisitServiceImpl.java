package org.aileme.shejiao.app.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.ProfileVisitService;
import org.aileme.shejiao.app.dao.ProfileVisitDao;
import org.aileme.shejiao.domain.entity.app.ProfileVisitEntity;
import org.aileme.shejiao.domain.vo.ProfileVisitorVo;

import java.util.Date;
import java.util.List;

/**
 * 个人主页访客埋点服务实现
 */
@Slf4j
@DS("master")
@Service("profileVisitService")
public class ProfileVisitServiceImpl extends ServiceImpl<ProfileVisitDao, ProfileVisitEntity> implements ProfileVisitService {

    @Autowired
    private ProfileVisitDao profileVisitDao;

    @Override
    public void recordVisit(Integer visitorUid, Integer targetUid) {
        if (visitorUid == null || targetUid == null || visitorUid.equals(targetUid)) {
            return;
        }

        // 查询是否已有访问记录
        ProfileVisitEntity exist = this.lambdaQuery()
                .eq(ProfileVisitEntity::getVisitorUid, visitorUid)
                .eq(ProfileVisitEntity::getTargetUid, targetUid)
                .one();

        Date now = new Date();
        if (exist != null) {
            // 已有记录，访问次数+1，更新最近访问时间
            exist.setVisitCount(exist.getVisitCount() + 1);
            exist.setLastVisitTime(now);
            exist.setUpdateTime(now);
            // 重算兴趣等级
            updateInterestLevel(exist);
            this.updateById(exist);
        } else {
            // 首次访问，新建记录
            ProfileVisitEntity entity = new ProfileVisitEntity();
            entity.setVisitorUid(visitorUid);
            entity.setTargetUid(targetUid);
            entity.setVisitCount(1);
            entity.setTotalDuration(0);
            entity.setLastVisitTime(now);
            entity.setInterestLevel(0);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            this.save(entity);
        }
    }

    @Override
    public void reportDuration(Integer visitorUid, Integer targetUid, Integer seconds) {
        if (visitorUid == null || targetUid == null || visitorUid.equals(targetUid) || seconds == null || seconds <= 0) {
            return;
        }

        ProfileVisitEntity exist = this.lambdaQuery()
                .eq(ProfileVisitEntity::getVisitorUid, visitorUid)
                .eq(ProfileVisitEntity::getTargetUid, targetUid)
                .one();

        if (exist != null) {
            exist.setTotalDuration(exist.getTotalDuration() + seconds);
            exist.setUpdateTime(new Date());
            // 重算兴趣等级
            updateInterestLevel(exist);
            this.updateById(exist);
        } else {
            // 理论上不应该出现（recordVisit 先于 reportDuration 调用）
            // 但做容错处理
            ProfileVisitEntity entity = new ProfileVisitEntity();
            entity.setVisitorUid(visitorUid);
            entity.setTargetUid(targetUid);
            entity.setVisitCount(1);
            entity.setTotalDuration(seconds);
            entity.setLastVisitTime(new Date());
            entity.setInterestLevel(0);
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
            updateInterestLevel(entity);
            this.save(entity);
        }
    }

    @Override
    public List<ProfileVisitorVo> getMyVisitors(Integer uid, Integer page, Integer size) {
        int offset = (page - 1) * size;
        return profileVisitDao.getVisitorList(uid, offset, size);
    }

    @Override
    public Integer getVisitorCount(Integer uid) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(ProfileVisitEntity::getTargetUid, uid)
                .count());
    }

    @Override
    public List<ProfileVisitorVo> getInterestedVisitors(Integer uid) {
        return profileVisitDao.getInterestedVisitors(uid);
    }

    @Override
    public List<ProfileVisitorVo> getRecentVisitors(Integer uid, Integer limit) {
        return profileVisitDao.getRecentVisitors(uid, limit);
    }

    /**
     * 根据访问次数和停留时长计算兴趣等级
     * 0: 访问1次 且 停留<15s（普通浏览）
     * 1: 访问>=2 或 停留>=15s（轻度兴趣）
     * 2: 访问>=3 且 停留>=30s（中度兴趣）
     * 3: 访问>=5 且 停留>=60s（高度兴趣）
     */
    private void updateInterestLevel(ProfileVisitEntity entity) {
        int count = entity.getVisitCount() != null ? entity.getVisitCount() : 0;
        int duration = entity.getTotalDuration() != null ? entity.getTotalDuration() : 0;

        int level = 0;
        if (count >= 5 && duration >= 60) {
            level = 3;
        } else if (count >= 3 && duration >= 30) {
            level = 2;
        } else if (count >= 2 || duration >= 15) {
            level = 1;
        }
        entity.setInterestLevel(level);
    }
}
