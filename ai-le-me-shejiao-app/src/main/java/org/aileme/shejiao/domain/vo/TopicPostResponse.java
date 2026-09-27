package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/5/15 16:02
 */
@Data
@Schema(name = "TopicPostResponse对象", description = "圈内帖子响应对象")
public class TopicPostResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "数量")
    private Integer number;

    @Schema(description = "圈子id")
    private Integer topicId;

}
