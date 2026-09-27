package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.TopicApplyService;
import org.aileme.shejiao.app.dao.TopicApplyDao;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.domain.entity.app.TopicApplyEntity;

import java.util.Map;

@DS("master")
@Service("topicApplyService")
public class TopicApplyServiceImpl extends ServiceImpl<TopicApplyDao, TopicApplyEntity> implements TopicApplyService {

    @Override
    public Boolean getApplyInfoByUserId(Integer uid, Integer topicId) {
        long count = this.lambdaQuery()
                .eq(TopicApplyEntity::getUid, uid)
                .eq(TopicApplyEntity::getTopicId, topicId)
                .count();
        return count > 0;
    }
}
