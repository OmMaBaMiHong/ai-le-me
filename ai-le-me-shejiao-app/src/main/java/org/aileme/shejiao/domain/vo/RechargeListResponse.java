package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author linfeng
 * @date 2022/4/19 16:48
 */
@Data
@AllArgsConstructor
@Schema(name = "RechargeListResponse", description = "充值选项列表响应体")
public class RechargeListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Integer id;

    @Schema(description = "价格")
    private Double price;

    @Schema(description = "赠送金额")
    private Double givePrice;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

}
