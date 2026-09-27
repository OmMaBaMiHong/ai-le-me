package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.admin.XiangqinEnrollmentEntity;

import java.util.List;
/**
 * 相亲局报名 DAO
 *
 * @author system
 * @date 2026-01-27
 */
@Mapper
public interface XiangqinEnrollmentDao extends BaseMapper<XiangqinEnrollmentEntity> {

    /**
     * 根据活动ID查询报名列表
     */
    List<XiangqinEnrollmentEntity> getByActivityId(@Param("activityId") Integer activityId);

    /**
     * 根据用户ID查询报名列表
     */
    List<XiangqinEnrollmentEntity> getByUserId(@Param("userId") Integer userId);

    /**
     * 检查用户是否已报名
     */
    XiangqinEnrollmentEntity checkEnrolled(@Param("activityId") Integer activityId, 
                                           @Param("userId") Integer userId);
}
