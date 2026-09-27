package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.UserTopicService;
import org.aileme.shejiao.app.dao.UserTopicDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.UserTopicEntity;

import java.util.List;
import java.util.Map;

/**
 * 用户话题服务实现类
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 21:24:46
 */
@DS("master")
@Service("userTopicService")
public class UserTopicServiceImpl extends ServiceImpl<UserTopicDao, UserTopicEntity> implements UserTopicService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<UserTopicEntity> page = this.page(
                new Query<UserTopicEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public Integer findUserTopicService(Integer topicId) {
        return Math.toIntExact(this.lambdaQuery()
                .eq(UserTopicEntity::getTopicId, topicId)
                .count());
    }

    @Override
    public Boolean isJoin(Integer uid, Integer id) {
        long count = this.lambdaQuery()
                .eq(UserTopicEntity::getUid, uid)
                .eq(UserTopicEntity::getTopicId, id)
                .count();
        return count > 0;
    }

    @Override
    public List<Integer> getUidByTopicId(Integer id) {
        return this.lambdaQuery()
                .eq(UserTopicEntity::getTopicId, id)
                .list()
                .stream()
                .map(UserTopicEntity::getUid)
                .toList();
    }

    @Override
    public List<Integer> getSomeUidListByTopicId(Integer id) {
        return this.lambdaQuery()
                .eq(UserTopicEntity::getTopicId, id)
                .last("LIMIT 10")
                .list()
                .stream()
                .map(UserTopicEntity::getUid)
                .toList();
    }

    @Override
    public List<UserTopicEntity> getTopicIdByUid(Integer uid) {
        return this.lambdaQuery()
                .eq(UserTopicEntity::getUid, uid)
                .list();
    }
}
