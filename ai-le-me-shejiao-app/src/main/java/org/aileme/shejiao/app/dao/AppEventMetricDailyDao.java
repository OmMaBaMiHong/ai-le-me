package org.aileme.shejiao.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.aileme.shejiao.domain.entity.app.AppEventMetricDailyEntity;

import java.util.Date;

/**
 * App 埋点日聚合 DAO
 */
@Mapper
public interface AppEventMetricDailyDao extends BaseMapper<AppEventMetricDailyEntity> {

    @Insert("INSERT INTO app_event_metric_daily(metric_date, event_name, module, page, event_count, create_time, update_time) " +
            "VALUES(#{metricDate}, #{eventName}, #{module}, #{page}, #{eventCount}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE event_count = event_count + #{eventCount}, update_time = NOW()")
    int upsertCount(@Param("metricDate") Date metricDate,
                    @Param("eventName") String eventName,
                    @Param("module") String module,
                    @Param("page") String page,
                    @Param("eventCount") Long eventCount);
}
