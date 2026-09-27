package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/5/7 15:51
 */
@Data
@AllArgsConstructor
@Schema(name = "SignConfigResponse", description = "配置列表响应体")
public class SignConfigResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Integer id;

    @Schema(description = "天数")
    private String day;

    @Schema(description = "签到数量")
    private String signNum;
}
