package org.aileme.shejiao.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.admin.XiangqinActivityEntity;

import java.util.List;
/**
 * 相亲局活动 DAO
 *
 * @author system
 * @date 2026-01-27
 */
@Mapper
public interface XiangqinActivityDao extends BaseMapper<XiangqinActivityEntity> {

    /**
     * 根据红娘ID查询活动列表
     */
    List<XiangqinActivityEntity> getByHongniangId(@Param("hongniangId") Integer hongniangId);

    /**
     * 增加浏览次数
     */
    void increaseViewCount(@Param("id") Integer id);

    /**
     * 更新报名人数
     */
    void updateEnrollCount(@Param("activityId") Integer activityId,
                          @Param("maleCount") Integer maleCount,
                          @Param("femaleCount") Integer femaleCount);

    /**
     * 获取App端活动列表（带红娘信息）
     */
    List<XiangqinActivityEntity> getAppActivityList(@Param("status") Integer status,
                                                    @Param("hongniangId") Integer hongniangId,
                                                    @Param("activityType") Integer activityType,
                                                    @Param("offset") long offset,
                                                    @Param("limit") long limit);

    /**
     * 根据ID获取活动详情（带红娘信息）
     */
    XiangqinActivityEntity getActivityWithHongniang(@Param("id") Integer id);
}
