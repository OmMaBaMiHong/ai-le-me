package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Table(name = "hongniang_match_case")
@TableName("hongniang_match_case")
@Schema(description = "红娘牵线案件")
public class HongniangMatchCaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer hongniangId;

    private Integer maleUserId;

    private Integer femaleUserId;

    private Integer currentStage;

    private Integer sourceType;

    private Long sourceRefId;

    private String sourceSnapshotJson;

    private Integer latestRequestStatus;

    private Date nextFollowTime;

    private Date lastFollowTime;

    private String closeReason;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @Transient
    @TableField(exist = false)
    private String hongniangName;

    @Transient
    @TableField(exist = false)
    private String maleUsername;

    @Transient
    @TableField(exist = false)
    private String maleMobile;

    @Transient
    @TableField(exist = false)
    private String femaleUsername;

    @Transient
    @TableField(exist = false)
    private String femaleMobile;

    @Transient
    @TableField(exist = false)
    private Integer groupCount;

    @Transient
    @TableField(exist = false)
    private String sourceDisplay;
}
