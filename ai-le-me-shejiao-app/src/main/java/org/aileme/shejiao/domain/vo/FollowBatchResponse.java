package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/5/13 20:45
 */
@Data
@Schema(name = "FollowBatchResponse对象", description = "用户关注响应对象")
public class FollowBatchResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private Integer id;

    @Schema(description = "用户id")
    private Integer uid;

    @Schema(description = "关注用户id")
    private Integer followUid;



}
