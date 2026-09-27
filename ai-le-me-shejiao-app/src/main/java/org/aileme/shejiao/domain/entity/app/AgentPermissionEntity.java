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
@Table(name = "agent_permission")
@TableName("agent_permission")
public class AgentPermissionEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private String capabilityCode;

    private Integer enabled;

    private String authorizeMode;

    private String targetScope;

    private String riskLevel;

    private Integer maxAmountPerAction;

    private Integer maxAmountPerDay;

    private Integer maxActionsPerDay;

    @Lob
    private String quietHoursJson;

    @Lob
    private String policyJson;

    private String consentVersion;

    private Date confirmedAt;

    private Date createTime;

    private Date updateTime;
}
