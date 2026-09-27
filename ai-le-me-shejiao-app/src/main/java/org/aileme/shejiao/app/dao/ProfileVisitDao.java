package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.app.ProfileVisitEntity;
import org.aileme.shejiao.domain.vo.ProfileVisitorVo;

import java.util.List;

/**
 * 个人主页访客埋点
 */
@Mapper
public interface ProfileVisitDao extends BaseMapper<ProfileVisitEntity> {

    /**
     * 查询谁看过我（关联用户表取头像/昵称等信息）
     */
    List<ProfileVisitorVo> getVisitorList(@Param("targetUid") Integer targetUid,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    /**
     * 查询对我感兴趣的人（interestLevel >= 2）
     */
    List<ProfileVisitorVo> getInterestedVisitors(@Param("targetUid") Integer targetUid);

    /**
     * 查询最近N个访客（用于主页预览）
     */
    List<ProfileVisitorVo> getRecentVisitors(@Param("targetUid") Integer targetUid,
                                             @Param("limit") Integer limit);
}
