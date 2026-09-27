package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "agent_action_task")
@TableName("agent_action_task")
public class AgentActionTaskEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Integer ownerUserId;

    private Integer targetUserId;

    private String capabilityCode;

    private String sceneCode;

    private String riskLevel;

    private String status;

    private Integer requiresApproval;

    private Long approvalId;

    private String runtimeTraceId;

    private String idempotencyKey;

    private String operatorMode;

    private Date scheduledTime;

    private Date expireTime;

    @Lob
    private String policySnapshotJson;

    @Lob
    private String planJson;

    @Lob
    private String executionJson;

    @Lob
    private String resultJson;

    private String errorMessage;

    private Date createTime;

    private Date updateTime;
}
