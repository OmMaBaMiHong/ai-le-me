package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.admin.TagsEntity;

import java.util.List;
/**
 * 标签 DAO
 *
 * @author system
 * @date 2026-01-27
 */
@Mapper
public interface TagsDao extends BaseMapper<TagsEntity> {

    /**
     * 根据分类查询标签
     */
    List<TagsEntity> getByCategory(@Param("tagCategory") String tagCategory);

    /**
     * 根据标签ID列表批量查询
     */
    List<TagsEntity> getBatchByIds(@Param("ids") List<Integer> ids);

    /**
     * 增加使用次数
     */
    void increaseUsageCount(@Param("id") Integer id);
}
