package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发布结果VO
 *
 * @author system
 * @date 2026-02-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PublishResultVo", description = "发布结果")
public class PublishResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "帖子ID")
    private Integer postId;
}
