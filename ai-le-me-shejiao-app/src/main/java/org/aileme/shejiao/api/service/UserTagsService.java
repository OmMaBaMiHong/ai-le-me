package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;
import org.aileme.shejiao.domain.entity.admin.UserTagsEntity;

import java.util.List;

/**
 * 用户标签关联Service
 *
 * @author system
 * @date 2026-01-27
 */
public interface UserTagsService extends IService<UserTagsEntity> {

    /**
     * 根据用户ID查询标签ID列表
     */
    List<Integer> getTagIdsByUserId(Integer userId);

    /**
     * 根据用户ID查询标签详情列表
     */
    List<TagsEntity> getTagsByUserId(Integer userId);

    /**
     * 根据标签ID查询用户ID列表
     */
    List<Integer> getUserIdsByTagId(Integer tagId);

    /**
     * 批量插入用户标签
     */
    void batchInsert(List<UserTagsEntity> list);

    /**
     * 设置用户标签（会先删除原有标签）
     */
    void setUserTags(Integer userId, List<Integer> tagIds, Integer sourceType);

    /**
     * 删除用户的所有标签
     */
    void deleteByUserId(Integer userId);
}
