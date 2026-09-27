package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * App 埋点事件明细
 */
@Data
@Entity
@Table(name = "app_event_track")
@TableName("app_event_track")
public class AppEventTrackEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String eventId;

    private String eventName;

    private String module;

    private String page;

    private Integer uid;

    private Integer targetUid;

    private String bizId;

    private String sourceType;

    private Long clientTs;

    private String traceId;

    private String propsJson;

    private Date eventDate;

    private Date createTime;

    private Date updateTime;
}
