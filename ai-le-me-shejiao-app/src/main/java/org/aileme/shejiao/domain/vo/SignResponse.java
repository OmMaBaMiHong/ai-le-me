package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/5/7 16:32
 */
@Data
@Schema(name = "SignResponse", description = "签到响应体")
public class SignResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "添加时间")
    private String addTime;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "数量")
    private Integer number;
}