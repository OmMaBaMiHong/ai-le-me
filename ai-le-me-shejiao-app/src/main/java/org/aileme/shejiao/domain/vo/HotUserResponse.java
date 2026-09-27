package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/10/30 14:48
 */
@Data
@Schema(name = "HotUserResponse对象", description = "热门用户响应对象")
public class HotUserResponse implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Integer uid;

    /**
     * 粉丝新增数
     */
    @Schema(description = "粉丝新增数")
    private Integer num;

}
