package org.aileme.shejiao.api.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.app.TopicApplyEntity;
public interface TopicApplyService extends IService<TopicApplyEntity> {
    Boolean getApplyInfoByUserId(Integer uid, Integer topicId);
}
