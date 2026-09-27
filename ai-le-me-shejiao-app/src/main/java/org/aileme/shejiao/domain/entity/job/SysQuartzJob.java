package org.aileme.shejiao.domain.entity.job;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 定时任务配置实体（对应 sys_quartz_job 表）
 */
@Data
@TableName("sys_quartz_job")
public class SysQuartzJob {

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_PAUSED = 0;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 业务分组，如 AI/SYSTEM
     */
    private String jobGroup;

    /**
     * 安全任务编码，由后端 handler 注册表解析
     */
    private String jobCode;

    /**
     * cron 表达式
     */
    private String cronExpression;

    /**
     * JSON 字符串参数
     */
    private String jobParams;

    /**
     * 1 允许并发，0 串行执行
     */
    private Integer allowConcurrent;

    /**
     * 1 启用，0 暂停
     */
    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Date nextFireTime;

    @TableField(exist = false)
    private Date previousFireTime;
}
