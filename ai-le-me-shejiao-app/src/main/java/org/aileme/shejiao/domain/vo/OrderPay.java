package org.aileme.shejiao.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单调取微信支付返回数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "OrderPay", description = "订单支付返回数据")
public class OrderPay {

    /**
     * appid
     */
    @Schema(description = "appid")
    private String appId;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳")
    private String timeStamp;

    /**
     * nonceStr
     */
    @Schema(description = "nonceStr")
    private String nonceStr;

    /**
     * package
     */
    @Schema(description = "package")
    @JsonProperty("package")
    private String packages;

    /**
     * 签名类型
     */
    @Schema(description = "签名类型")
    private String signType;

    /**
     * 签名
     */
    @Schema(description = "签名")
    private String paySign;


    /**
     * h5支付链接
     */
    @Schema(description = "h5支付链接")
    private String mwebUrl;

    /**
     * 商户号
     */
    @Schema(description = "商户号")
    private String partnerId;

    @Schema(description = "支付渠道")
    private String payChannel;

    @Schema(description = "是否mock支付")
    private Boolean mockMode;

    @Schema(description = "订单号")
    private String orderId;

}
