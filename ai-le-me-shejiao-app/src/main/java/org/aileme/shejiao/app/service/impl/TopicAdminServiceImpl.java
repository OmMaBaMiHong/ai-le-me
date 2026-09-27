package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.TopicAdminService;
import org.aileme.shejiao.app.dao.TopicAdminDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.TopicAdminEntity;

import java.util.Map;

/**
 * 话题管理员服务实现类
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 20:33:29
 */
@DS("master")
@Service("topicAdminService")
public class TopicAdminServiceImpl extends ServiceImpl<TopicAdminDao, TopicAdminEntity> implements TopicAdminService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<TopicAdminEntity> page = this.page(
                new Query<TopicAdminEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public Boolean isAdmin(Integer uid, Integer id) {
        long count = this.lambdaQuery()
                .eq(TopicAdminEntity::getUid, uid)
                .eq(TopicAdminEntity::getTopicId, id)
                .count();
        return count > 0;
    }
}
