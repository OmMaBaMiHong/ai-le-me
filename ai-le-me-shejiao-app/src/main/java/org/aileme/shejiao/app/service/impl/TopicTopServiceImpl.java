package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.TopicTopService;
import org.aileme.shejiao.app.dao.TopicTopDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.TopicTopEntity;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

@DS("master")
@Service("topicTopService")
public class TopicTopServiceImpl extends ServiceImpl<TopicTopDao, TopicTopEntity> implements TopicTopService {

    @Resource
    private TopicTopDao topicTopDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<TopicTopEntity> page = this.page(
                new Query<TopicTopEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<TopicTopEntity> findByTopicId(Integer id) {
        return this.lambdaQuery()
                .eq(TopicTopEntity::getTopicId, id)
                .list();
    }
}
