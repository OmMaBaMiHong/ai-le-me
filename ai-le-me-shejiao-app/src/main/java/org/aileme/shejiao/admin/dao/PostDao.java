package org.aileme.shejiao.admin.dao;

import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.vo.ChartDataResponse;
import org.aileme.shejiao.domain.vo.TopicPostResponse;
import org.aileme.shejiao.domain.entity.admin.PostEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
/**
 * 
 * 
 * @author linfeng
 * @email 2445465217@qq.com
 * @date 2022-01-23 20:49:55
 */
@Mapper
public interface PostDao extends BaseMapper<PostEntity> {

    List<TopicPostResponse> findTopicPostCountBatch(List<Integer> topicIdList);

    @Select("SELECT count(id) as num," +
            "DATE_FORMAT(create_time, '%m-%d') as time " +
            " FROM post where status=0 and create_time >= #{time}" +
            " GROUP BY DATE_FORMAT(create_time,'%Y-%m-%d') " +
            " ORDER BY create_time ASC")
    List<ChartDataResponse> chartList(Date nowMonth);

    @Select("select media from post where uid=${uid} and type=1 and media is not null  limit 6")
    List<String> getImagePostListByUser(@Param("uid") Integer uid);
}
