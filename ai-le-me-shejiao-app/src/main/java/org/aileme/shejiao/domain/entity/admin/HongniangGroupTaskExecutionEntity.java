package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "hongniang_group_task_execution")
@TableName("hongniang_group_task_execution")
public class HongniangGroupTaskExecutionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer taskId;

    private Integer groupId;

    private Integer providerType;

    private Integer executionStatus;

    private String requestPayload;

    private String providerTaskId;

    private String providerResponse;

    private String resultSummary;

    private Date executeTime;

    private Date finishTime;

    private Date createTime;

    private Date updateTime;
}
