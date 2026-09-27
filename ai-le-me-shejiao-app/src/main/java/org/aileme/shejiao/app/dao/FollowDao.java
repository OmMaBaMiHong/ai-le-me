package org.aileme.shejiao.app.dao;

import cn.hutool.core.date.DateTime;
import org.aileme.shejiao.domain.vo.FollowBatchResponse;
import org.aileme.shejiao.domain.vo.HotUserResponse;
import org.aileme.shejiao.domain.entity.app.FollowEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-24 14:38:31
 */
@Mapper
public interface FollowDao extends BaseMapper<FollowEntity> {

	void cancelFollow(@Param("uid")Integer uid, @Param("followId")Integer followId);

    List<FollowBatchResponse> findFollowBatch(@Param("list")List<Integer> list,@Param("uid")Integer uid);


    @Select("SELECT follow_uid AS uid,COUNT(follow_uid) AS num FROM follow " +
            " WHERE create_time >= #{time} " +
            " GROUP BY follow_uid " +
            " ORDER BY num DESC LIMIT 5")
    List<HotUserResponse> getHotUserList(DateTime dateTime);

    @Select("SELECT follow_uid  FROM follow " +
            " WHERE uid = #{uid}  and follow_uid>= #{recommendUid} limit 10 "
       )
    List<Integer> queryFollows(@Param("uid")Integer uid, @Param("recommendUid")Integer recommendUid);
}
