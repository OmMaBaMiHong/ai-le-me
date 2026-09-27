package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author linfeng
 * @date 2022/5/2 20:09
 */
@Data
@Schema(name = "BillResponse", description = "账单信息响应体")
public class BillResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 明细数字
     */
    @Schema(description = "明细数字")
    private BigDecimal number;
    /**
     * 添加时间
     */
    @Schema(description = "添加时间")
    private Date addTime;
    /**
     * 0 = 支出 1 = 获得
     */
    @Schema(description = "0 = 支出 1 = 获得")
    private Integer pm;
    /**
     * 账单标题
     */
    @Schema(description = "账单标题")
    private String title;

    @Schema(description = "时间")
    private String time;
}
