package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author linfeng
 * @date 2022/7/21 10:09
 */
@Data
@Schema(name = "ChartDataResponse", description = "曲线图数据响应体")
public class ChartDataResponse {

    @Schema(description = "数量")
    private  Double num;

    @Schema(description = "时间")
    private  String time;
}
