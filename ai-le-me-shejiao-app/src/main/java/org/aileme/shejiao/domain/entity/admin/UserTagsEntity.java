package org.aileme.shejiao.domain.entity.admin;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户标签关联实体类
 *
 * @author system
 * @date 2026-01-27
 */
@Data
@Entity
@Table(name = "user_tags")
@TableName("user_tags")
@Schema(description = "用户标签关联实体")
public class UserTagsEntity implements Serializable {
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
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Integer userId;

    /**
     * 标签ID
     */
    @Schema(description = "标签ID")
    private Integer tagId;

    /**
     * 来源：1-用户自选 2-红娘添加 3-AI推荐
     */
    @Schema(description = "来源：1-用户自选 2-红娘添加 3-AI推荐")
    private Integer sourceType;

    /**
     * 权重（0.00-1.00）
     */
    @Schema(description = "权重")
    private BigDecimal weight;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;
}
