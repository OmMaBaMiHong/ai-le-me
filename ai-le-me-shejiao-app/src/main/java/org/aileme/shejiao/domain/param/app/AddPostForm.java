package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;


@Data
@Schema(name = "发布帖子")
public class AddPostForm implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 圈子id
     */
    @NotNull(message = "无参数圈子id")
    @Schema(description = "圈子id")
    private Integer topicId;
    /**
     * 话题id
     */
    @Schema(description = "话题id")
    private Integer discussId;

    /**
     * 关联活动id
     */
    @Schema(description = "关联活动id")
    private Integer activityId;

    /**
     * 标题
     */
//    @Length(max = 20, message = "标题不能超过20个字符")
//    @NotBlank(message = "参数有误")
    @Schema(description = "标题")
    private String title;
    /**
     * 内容
     */
    @Length(max = 400, message = "内容不能超过400个字符")
    @NotBlank(message = "参数有误")
    @Schema(description = "内容")
    private String content;
    /**
     * 文件
     */
    @Schema(description = "文件")
    private List<String> media;

    /**
     * 帖子类型：1 图文 ，2视频 ，3文章，4投票
     */
    @NotNull(message = "无参数type")
    @Schema(description = "帖子类型：1 图文 ，2视频 ，3文章，4投票")
    private Integer type;
    /**
     * 地址名称
     */
    @Schema(description = "地址名称")
    private String address;
    /**
     * 经度
     */
    @Schema(description = "经度")
    private Double longitude;
    /**
     * 纬度
     */
    @Schema(description = "纬度")
    private Double latitude;
    /**
     * 0 普通贴  1 付费贴  2 红包贴
     */
    @Schema(description = "0 普通贴  1 付费贴  2 红包贴")
    private Integer cut;
    /**
     * 付费贴支付金额
     */
    @Schema(description = "付费贴支付金额")
    private BigDecimal pay;

    /**
     * 付费简介
     */
    @Length(max = 250, message = "简介不能超过250个字符")
    @Schema(description = "付费简介")
    private String brief;

    /**
     * 话题ID列表
     */
    @Schema(description = "话题ID列表")
    private List<Integer> tagIds;

}
