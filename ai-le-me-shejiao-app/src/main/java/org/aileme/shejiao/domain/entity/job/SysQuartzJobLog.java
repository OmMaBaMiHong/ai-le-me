package org.aileme.shejiao.domain.entity.job;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志
 */
@Data
@TableName("sys_quartz_job_log")
public class SysQuartzJobLog {

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAIL = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private String jobName;

    private String jobCode;

    private Integer executeStatus;

    private String resultSummary;

    private String errorMessage;

    private Long durationMs;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
