package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.admin.HongniangUserRelationEntity;

import java.util.List;
/**
 * 红娘-用户关联 DAO
 *
 * @author system
 * @date 2026-01-27
 */
@Mapper
public interface HongniangUserRelationDao extends BaseMapper<HongniangUserRelationEntity> {

    /**
     * 根据红娘ID查询关联的用户ID列表
     */
    List<Integer> getUserIdsByHongniangId(@Param("hongniangId") Integer hongniangId);

    /**
     * 根据用户ID查询关联的红娘ID列表
     */
    List<Integer> getHongniangIdsByUserId(@Param("userId") Integer userId);

    /**
     * 批量插入关联关系
     */
    void batchInsert(List<HongniangUserRelationEntity> list);
}
