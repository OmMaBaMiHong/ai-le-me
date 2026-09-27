package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.aileme.shejiao.domain.entity.app.AppEventTrackEntity;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * App 埋点事件明细 DAO
 */
@Mapper
public interface AppEventTrackDao extends BaseMapper<AppEventTrackEntity> {

    @Insert("INSERT IGNORE INTO app_event_track(event_id, event_name, module, page, uid, target_uid, biz_id, source_type, client_ts, trace_id, props_json, event_date, create_time, update_time) " +
            "VALUES(#{eventId}, #{eventName}, #{module}, #{page}, #{uid}, #{targetUid}, #{bizId}, #{sourceType}, #{clientTs}, #{traceId}, #{propsJson}, #{eventDate}, NOW(), NOW())")
    int insertIgnore(AppEventTrackEntity entity);

    @Select("SELECT id, event_id AS eventId, event_name AS eventName, module, page, uid, target_uid AS targetUid, biz_id AS bizId, source_type AS sourceType, client_ts AS clientTs, trace_id AS traceId, props_json AS propsJson, event_date AS eventDate, create_time AS createTime, update_time AS updateTime " +
            "FROM app_event_track WHERE uid = #{uid} ORDER BY id DESC LIMIT #{limit}")
    List<AppEventTrackEntity> selectRecentByUid(@Param("uid") Integer uid, @Param("limit") Integer limit);

    @Select("SELECT event_name AS eventName, SUM(event_count) AS totalCount FROM app_event_metric_daily WHERE metric_date BETWEEN #{from} AND #{to} GROUP BY event_name ORDER BY totalCount DESC")
    List<Map<String, Object>> selectFunnelByDate(@Param("from") Date from, @Param("to") Date to);
}
