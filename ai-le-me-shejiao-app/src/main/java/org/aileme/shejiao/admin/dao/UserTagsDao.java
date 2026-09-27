package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.admin.UserTagsEntity;

import java.util.List;
/**
 * 用户标签关联 DAO
 *
 * @author system
 * @date 2026-01-27
 */
@Mapper
public interface UserTagsDao extends BaseMapper<UserTagsEntity> {

    /**
     * 根据用户ID查询标签ID列表
     */
    List<Integer> getTagIdsByUserId(@Param("userId") Integer userId);

    /**
     * 根据标签ID查询用户ID列表
     */
    List<Integer> getUserIdsByTagId(@Param("tagId") Integer tagId);

    /**
     * 批量插入用户标签
     */
    void batchInsert(List<UserTagsEntity> list);

    /**
     * 删除用户的所有标签
     */
    void deleteByUserId(@Param("userId") Integer userId);
}
