package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/5/13 20:45
 */
@Data
@Schema(name = "PostIsCollectionResponse", description = "帖子数量响应体")
public class PostIsCollectionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Integer uid;

    @Schema(description = "帖子id")
    private Integer postId;

}
