
package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@Schema(name = "用户添加评论")
public class AddCommentForm {


    @Schema(description = "pid")
    private Integer pid;

    @Schema(description = "type")
    private Integer type;

    @Schema(description = "toUid")
    private Integer toUid;

    @NotNull(message = "帖子ID不能为空")
    @Schema(description = "postId")
    private Integer postId;

    @Length(max = 50, message = "评论内容不能超过50个字符")
    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "content")
    private String content;

    @Schema(description = "media")
    private List<String> media;



}
