package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.app.UserImpressionTagEntity;

import java.util.List;
import java.util.Map;

/**
 * 用户印象标签 Service
 */
public interface UserImpressionTagService extends IService<UserImpressionTagEntity> {

    /**
     * 保存当前用户对目标用户的一组印象标签（会覆盖之前的记录）。
     */
    void saveUserImpressions(Integer fromUserId, Integer toUserId, List<Integer> tagIds, String sourceType, Integer sourceId);

    /**
     * 获取某个用户的印象标签聚合结果：key 为 tagId，value 为计数。
     */
    Map<Integer, Integer> getImpressionSummary(Integer toUserId);
}
