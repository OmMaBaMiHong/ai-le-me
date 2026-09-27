package org.aileme.shejiao.admin.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.aileme.shejiao.admin.dao.HongniangDao;
import org.aileme.shejiao.admin.dao.HongniangUserRelationDao;
import org.aileme.shejiao.api.service.HongniangUserRelationService;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 红娘-用户关联Service实现
 *
 * @author system
 * @date 2026-01-27
 */
@DS("master")
@Service("hongniangUserRelationService")
public class HongniangUserRelationServiceImpl extends ServiceImpl<HongniangUserRelationDao, HongniangUserRelationEntity> implements HongniangUserRelationService {

    @Autowired
    private HongniangUserRelationDao relationDao;

    @Autowired
    private HongniangDao hongniangDao;

    @Override
    public List<Integer> getUserIdsByHongniangId(Integer hongniangId) {
        return this.lambdaQuery()
                .eq(HongniangUserRelationEntity::getHongniangId, hongniangId)
                .list()
                .stream()
                .map(HongniangUserRelationEntity::getUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getHongniangIdsByUserId(Integer userId) {
        return this.lambdaQuery()
                .eq(HongniangUserRelationEntity::getUserId, userId)
                .list()
                .stream()
                .map(HongniangUserRelationEntity::getHongniangId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @DSTransactional
    public void batchInsert(List<HongniangUserRelationEntity> list) {
        if (list != null && !list.isEmpty()) {
            fillMissingHongniangUserNos(list);
            this.saveBatch(list);
        }
    }

    @Override
    @DSTransactional
    public void saveRelationWithAutoNo(HongniangUserRelationEntity relation) {
        if (relation == null) {
            return;
        }
        fillMissingHongniangUserNos(List.of(relation));
        if (relation.getCreateTime() == null) {
            relation.setCreateTime(new Date());
        }
        this.save(relation);
    }

    @Override
    public int getNextHongniangUserNo(Integer hongniangId) {
        if (hongniangId == null || hongniangId <= 0) {
            return 1;
        }
        HongniangUserRelationEntity latestRelation = this.lambdaQuery()
                .eq(HongniangUserRelationEntity::getHongniangId, hongniangId)
                .orderByDesc(HongniangUserRelationEntity::getHongniangUserNo)
                .orderByDesc(HongniangUserRelationEntity::getId)
                .last("limit 1")
                .one();
        if (latestRelation == null || latestRelation.getHongniangUserNo() == null || latestRelation.getHongniangUserNo() < 1) {
            return 1;
        }
        return latestRelation.getHongniangUserNo() + 1;
    }

    @Override
    public boolean hasPermission(Integer hongniangId, Integer userId) {
        List<Integer> userIds = getUserIdsByHongniangId(hongniangId);
        return userIds.contains(userId);
    }

    @Override
    public boolean checkFollowed(Integer userId, Integer hongniangId) {
        QueryWrapper<HongniangUserRelationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("hongniang_id", hongniangId);
        return this.count(wrapper) > 0;
    }

    @Override
    @DSTransactional
    public void follow(Integer userId, Integer hongniangId) {
        // 检查是否已存在
        if (checkFollowed(userId, hongniangId)) {
            return;
        }

        // 插入关注关系
        HongniangUserRelationEntity relation = new HongniangUserRelationEntity();
        relation.setUserId(userId);
        relation.setHongniangId(hongniangId);
        relation.setSourceType(2); // 2-用户主动关联
        relation.setCreateTime(new Date());
        saveRelationWithAutoNo(relation);
    }

    @Override
    @DSTransactional
    public void unfollow(Integer userId, Integer hongniangId) {
        QueryWrapper<HongniangUserRelationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("hongniang_id", hongniangId);
        this.remove(wrapper);
    }

    @Override
    public List<HongniangInfoEntity> getFollowedHongniangList(Integer userId) {
        // 获取关注的红娘ID列表
        List<Integer> hongniangIds = getHongniangIdsByUserId(userId);
        if (hongniangIds == null || hongniangIds.isEmpty()) {
            return List.of();
        }

        // 查询红娘详情
        return hongniangDao.selectBatchIds(hongniangIds);
    }

    private void fillMissingHongniangUserNos(Collection<HongniangUserRelationEntity> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }
        Map<Integer, Integer> nextNoByHongniang = new HashMap<>();
        for (HongniangUserRelationEntity relation : relations) {
            if (relation == null || relation.getHongniangId() == null || relation.getHongniangId() <= 0) {
                continue;
            }
            if (relation.getHongniangUserNo() != null && relation.getHongniangUserNo() > 0) {
                nextNoByHongniang.merge(relation.getHongniangId(), relation.getHongniangUserNo() + 1, Math::max);
                continue;
            }
            Integer nextNo = nextNoByHongniang.computeIfAbsent(relation.getHongniangId(), this::getNextHongniangUserNo);
            relation.setHongniangUserNo(nextNo);
            nextNoByHongniang.put(relation.getHongniangId(), nextNo + 1);
        }
    }
}
