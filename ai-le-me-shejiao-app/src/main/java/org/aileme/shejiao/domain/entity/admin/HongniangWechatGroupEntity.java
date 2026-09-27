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
@Table(name = "hongniang_wechat_group")
@TableName("hongniang_wechat_group")
@Schema(description = "红娘微信群资产")
public class HongniangWechatGroupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer hongniangId;

    private String groupName;

    private Integer groupType;

    private String ownerName;

    private String ownerWechat;

    private String tagJson;

    private String city;

    private String purpose;

    private String qrCodeUrl;

    private String joinLink;

    private String externalGroupId;

    private Integer providerType;

    private Integer syncStatus;

    private Date lastSyncTime;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @Transient
    @TableField(exist = false)
    private String hongniangName;

    @Transient
    @TableField(exist = false)
    private Integer boundUserCount;

    @Transient
    @TableField(exist = false)
    private Integer boundCaseCount;
}
