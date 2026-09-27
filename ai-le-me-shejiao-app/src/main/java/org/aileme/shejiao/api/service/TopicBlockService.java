package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.app.TopicBlockEntity;

import java.util.Map;

/**
 * 圈子用户拉黑
 *
 * @author pity
 * @email linfengtech002@163.com
 * @date 2023-06-25 13:40:04
 */
public interface TopicBlockService extends IService<TopicBlockEntity> {

    PageUtils queryPage(Map<String, Object> params);

    Boolean isBlock(Integer topicId, Integer uid);

    void addBlock(Integer topicId, Integer uid, Integer operateId);

    void removeBlock(Integer uid, Integer topicId);
}
