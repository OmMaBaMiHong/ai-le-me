package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.aileme.shejiao.domain.entity.admin.HongniangInfoEntity;
/**
 * 红娘 DAO
 *
 * @author system
 * @date 2026-01-27
 */
@Mapper
public interface HongniangDao extends BaseMapper<HongniangInfoEntity> {

    /**
     * 根据手机号查询红娘
     */
    @Select("""
        SELECT *
        FROM hongniang_info
        WHERE phone = #{phone}
        LIMIT 1
        """)
    HongniangInfoEntity getByPhone(@Param("phone") String phone);

    /**
     * 根据用户ID查询红娘
     */
    @Select("""
        SELECT *
        FROM hongniang_info
        WHERE user_id = #{userId}
        LIMIT 1
        """)
    HongniangInfoEntity getByUserId(@Param("userId") Integer userId);

    /**
     * 更新统计数据
     */
    @Update("""
        UPDATE hongniang_info
        SET total_users = #{totalUsers},
            total_activities = #{totalActivities},
            success_count = #{successCount},
            update_time = NOW()
        WHERE id = #{id}
        """)
    void updateStatistics(@Param("id") Integer id, 
                         @Param("totalUsers") Integer totalUsers,
                         @Param("totalActivities") Integer totalActivities,
                         @Param("successCount") Integer successCount);
}
