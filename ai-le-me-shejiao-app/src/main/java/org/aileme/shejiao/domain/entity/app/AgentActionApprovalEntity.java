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
@Table(name = "agent_action_approval")
@TableName("agent_action_approval")
public class AgentActionApprovalEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer ownerUserId;

    private Integer targetUserId;

    private String capabilityCode;

    private String riskLevel;

    private String status;

    @Lob
    private String approvalPayloadJson;

    private String decisionNote;

    private Integer decisionByUserId;

    private Date requestedAt;

    private Date decidedAt;

    private Date expireTime;

    private Date createTime;

    private Date updateTime;
}
