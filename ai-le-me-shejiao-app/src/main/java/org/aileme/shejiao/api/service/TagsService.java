package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.common.utils.PageUtils;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;

import java.util.List;
import java.util.Map;

/**
 * 标签Service
 *
 * @author system
 * @date 2026-01-27
 */
public interface TagsService extends IService<TagsEntity> {

    /**
     * 分页查询标签列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存标签
     */
    void saveTag(TagsEntity tag);

    /**
     * 更新标签
     */
    void updateTag(TagsEntity tag);

    /**
     * 根据分类查询标签
     */
    List<TagsEntity> getByCategory(String tagCategory);

    /**
     * 根据ID列表批量查询
     */
    List<TagsEntity> getBatchByIds(List<Integer> ids);

    /**
     * 获取所有标签分类
     */
    List<String> getAllCategories();

    /**
     * 增加使用次数
     */
    void increaseUsageCount(Integer tagId);
}
