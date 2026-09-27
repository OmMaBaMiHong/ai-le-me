package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/5/13 20:45
 */
@Data
@Schema(name = "CommentCountResponse", description = "评论数量响应体")
public class CommentCountResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "数量")
    private Integer number;

    @Schema(description = "帖子id")
    private Integer postId;

}
