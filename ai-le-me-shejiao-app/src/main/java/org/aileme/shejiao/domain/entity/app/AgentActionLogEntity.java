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
@Table(name = "agent_action_log")
@TableName("agent_action_log")
public class AgentActionLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String traceId;

    private String requestId;

    private Long taskId;

    private Long approvalId;

    private Integer ownerUserId;

    private Integer targetUserId;

    private String capabilityCode;

    private String eventType;

    private String eventStatus;

    private String sourceType;

    private String riskLevel;

    private String message;

    @Lob
    private String payloadJson;

    private Date createTime;
}
