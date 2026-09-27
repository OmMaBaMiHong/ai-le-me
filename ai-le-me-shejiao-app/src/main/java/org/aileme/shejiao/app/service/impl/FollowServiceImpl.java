package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.FollowService;
import org.aileme.shejiao.app.dao.FollowDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.entity.app.FollowEntity;
import org.aileme.shejiao.domain.vo.FollowBatchResponse;
import org.aileme.shejiao.domain.vo.HotUserResponse;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@DS("master")
@Service("followService")
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowDao, FollowEntity> implements FollowService {

    @Resource
    private FollowDao followDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<FollowEntity> page = this.page(
                new Query<FollowEntity>().getPage(params),
                new QueryWrapper<FollowEntity>()
        );
        return new PageUtils(page);
    }

    @Override
    public Integer isFollow(Integer uid, Integer followUid) {
        long count = this.lambdaQuery()
                .eq(FollowEntity::getUid, uid)
                .eq(FollowEntity::getFollowUid, followUid)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .count();
        return count > 0 ? 1 : 0;
    }

    @Override
    public boolean isFollowOrNot(Integer uid, Integer followUid) {
        long count = this.lambdaQuery()
                .eq(FollowEntity::getUid, uid)
                .eq(FollowEntity::getFollowUid, followUid)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .count();
        return count > 0;
    }

    @Override
    public List<FollowBatchResponse> findFollowBatch(List<Integer> list, Integer uid) {
        List<FollowEntity> entities = this.lambdaQuery()
                .eq(FollowEntity::getUid, uid)
                .in(FollowEntity::getFollowUid, list)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .list();
        
        return entities.stream().map(entity -> {
            FollowBatchResponse response = new FollowBatchResponse();
            response.setId(entity.getId());
            response.setUid(entity.getUid());
            response.setFollowUid(entity.getFollowUid());
            return response;
        }).toList();
    }

    @Override
    public List<Integer> getFollowUids(AppUserEntity user) {
        return this.lambdaQuery()
                .eq(FollowEntity::getUid, user.getUid())
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .list()
                .stream()
                .map(FollowEntity::getFollowUid)
                .toList();
    }

    @Override
    public Integer getFollowCount(Integer uid) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(FollowEntity::getUid, uid)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .count());
    }

    @Override
    public Integer getFans(Integer uid) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(FollowEntity::getFollowUid, uid)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .count());
    }

    @Override
    public List<Integer> getFansList(Integer uid) {
        return this.lambdaQuery()
                .eq(FollowEntity::getFollowUid, uid)
                .and(wrapper -> wrapper.eq(FollowEntity::getStatus, 0).or().isNull(FollowEntity::getStatus))
                .list()
                .stream()
                .map(FollowEntity::getUid)
                .toList();
    }

    @Override
    public List<Integer> queryFollows(Integer uid, Integer recommendUid) {
        return followDao.queryFollows(uid, recommendUid);
    }

    @Override
    public List<HotUserResponse> getHotUserList(DateTime dateTime) {
        return followDao.getHotUserList(dateTime);
    }
}
