package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * App 埋点日聚合
 */
@Data
@Entity
@Table(name = "app_event_metric_daily")
@TableName("app_event_metric_daily")
public class AppEventMetricDailyEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Date metricDate;

    private String eventName;

    private String module;

    private String page;

    private Long eventCount;

    private Date createTime;

    private Date updateTime;
}
