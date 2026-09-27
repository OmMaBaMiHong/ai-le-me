package org.aileme.shejiao.domain.entity.app;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.persistence.*;

import java.util.Date;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户推荐设置
 * </p>
 *
 * @author lww
 * @since 2023-04-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(name="recommend_love", description="用户推荐设置")
@Entity
@Table(name = "recommend_love")
@TableName("recommend_love")
public class RecommendLoveEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
      @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @Schema(description = "用户id")
    private Integer uid;

    @Schema(description = "最新被推荐的用户id")
    private Integer recommendUid;

    @Schema(description = "推城市")
    private String cityids;

    @Schema(description = "推学历范围")
    private String edu;

    @Schema(description = "推身高范围")
    private String height;

    @Schema(description = "推年龄段")
    private String age;

    @Schema(description = "优先推关注我的")
    private Integer recFollowus;

    @Schema(description = "优先推我给喜欢的人")
    private Integer recBefollowus;

    @Schema(description = "只推我给喜欢的人")
    private Integer recOnlyBefollowus;

    @Schema(description = "创建时间")
      @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "剩余推荐人数")
    private Integer recNum;

    private Integer vip;
    @Schema(description = "已推荐总人数")
    private Integer hasRecNums;

}
