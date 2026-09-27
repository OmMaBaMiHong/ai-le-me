package org.aileme.shejiao.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 获取公众号二维码地址
 *
 * @author zwb
 * @version 1.0
 * @date 2023-04-06  11:10
 */
@Data
public class WxMpQrVO {

    @Schema(description = "ticket", title = "获取二维码地址：https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=返回的ticket")
    protected String ticket;

    @Schema(description = "有效期")
    protected int expireSeconds;

    @Schema(description = "url")
    protected String url;

    @Schema(description = "场景ID", title = "通过sceneId轮询1s调用是否登录接口")
    protected String sceneId;

    private  String token;

}
