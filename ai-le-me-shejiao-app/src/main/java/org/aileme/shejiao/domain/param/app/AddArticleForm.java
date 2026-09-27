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
@Schema(name = "发布文章")
public class AddArticleForm implements Serializable {

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
     * 标题
     */
    @Length(max = 20, message = "标题不能超过20个字符")
    @NotBlank(message = "参数有误")
    @Schema(description = "标题")
    private String title;
    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;


    /**
     * 帖子类型：1 图文 ，2视频 ，3文章，4投票
     */
    @NotNull(message = "无参数type")
    @Schema(description = "帖子类型：1 图文 ，2视频 ，3文章，4投票")
    private Integer type;

    /**
     * 封面图
     */
    @Schema(description = "封面图")
    private List<String> media;
}
