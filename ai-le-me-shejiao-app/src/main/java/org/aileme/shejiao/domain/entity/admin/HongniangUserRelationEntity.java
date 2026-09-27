package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 红娘-用户关联实体类
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@Entity
@Table(name = "hongniang_user_relation")
@TableName("hongniang_user_relation")
@Schema(description = "红娘-用户关联实体")
public class HongniangUserRelationEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "关联ID")
    private Integer id;

    /**
     * 红娘ID
     */
    @Schema(description = "红娘ID")
    private Integer hongniangId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 红娘内用户编号
     */
    @Schema(description = "红娘内用户编号")
    private Integer hongniangUserNo;

    /**
     * 来源类型：1-红娘导入 2-用户主动关联 3-系统分配
     */
    @Schema(description = "来源类型：1-红娘导入 2-用户主动关联 3-系统分配")
    private Integer sourceType;

    /**
     * 导入文件名
     */
    @Schema(description = "导入文件名")
    private String importFileName;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息")
    private String remark;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
