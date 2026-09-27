package org.aileme.shejiao.domain.param.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "活动报名支付表单")
public class ActivityEnrollPayForm implements Serializable {

    @NotNull(message = "活动ID不能为空")
    @Schema(description = "活动ID")
    private Integer activityId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "微信号")
    private String wechat;

    @Schema(description = "报名备注")
    private String enrollRemark;

    @Schema(description = "支付方式 weixin/h5/app/wxh5")
    private String payType;

    @Schema(description = "支付渠道 wechat/alipay")
    private String payChannel;
}
