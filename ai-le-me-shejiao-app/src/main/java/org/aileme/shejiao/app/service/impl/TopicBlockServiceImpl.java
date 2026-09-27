package org.aileme.shejiao.app.service.impl;
import com.baomidou.dynamic.datasource.annotation.DS;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.DateUtil;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.common.utils.Query;
import org.aileme.shejiao.app.dao.TopicBlockDao;
import org.aileme.shejiao.domain.entity.app.TopicBlockEntity;
import org.aileme.shejiao.api.service.TopicBlockService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@DS("master")
@Service("topicBlockService")
public class TopicBlockServiceImpl extends ServiceImpl<TopicBlockDao, TopicBlockEntity> implements TopicBlockService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<TopicBlockEntity> page = this.page(
                new Query<TopicBlockEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    /**
     * 判断用户是否被圈子拉入黑名单
     * @param topicId
     * @param uid
     * @return
     */
    @Override
    public Boolean isBlock(Integer topicId, Integer uid) {
        List<TopicBlockEntity> list = this.lambdaQuery()
                .eq(TopicBlockEntity::getUid, uid)
                .eq(TopicBlockEntity::getTopicId, topicId)
                .list();

        return list.size()>0;
    }

    @Override
    public void addBlock(Integer topicId, Integer uid, Integer operateId) {
        TopicBlockEntity block=new TopicBlockEntity();
        block.setCreateTime(DateUtil.nowDateTime());
        block.setOperateId(operateId);
        block.setUid(uid);
        block.setTopicId(topicId);
        boolean save = this.save(block);
        if(!save){
            throw new LinfengException("列入黑名单失败");
        }
    }

    @Override
    public void removeBlock(Integer uid, Integer topicId) {
        boolean remove = this.remove(new LambdaQueryWrapper<TopicBlockEntity>()
                .eq(TopicBlockEntity::getTopicId, topicId)
                .eq(TopicBlockEntity::getUid, uid));
        if(!remove){
            throw new LinfengException("移出黑名单失败");
        }
    }

}
