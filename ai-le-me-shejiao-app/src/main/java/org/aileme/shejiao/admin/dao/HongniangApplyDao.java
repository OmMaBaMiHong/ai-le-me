package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.admin.HongniangApplyEntity;

/**
 * 红娘申请 DAO
 *
 * @author system
 * @date 2026-02-10
 */
@Mapper
public interface HongniangApplyDao extends BaseMapper<HongniangApplyEntity> {

    /**
     * 根据用户ID查询最新申请
     */
    HongniangApplyEntity getLatestByUserId(@Param("userId") Integer userId);
}
