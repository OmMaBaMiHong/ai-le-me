package org.aileme.shejiao.app.service;

import org.aileme.shejiao.domain.vo.TagProfileAggregateVO;

/**
 * 标签画像聚合服务
 */
public interface TagProfileAggregateService {

    /**
     * 聚合用户标签画像
     */
    TagProfileAggregateVO aggregate(Integer userId);
}
