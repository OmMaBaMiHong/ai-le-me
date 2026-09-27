package org.aileme.shejiao.api.service;

/**
 * 微信运行时配置统一读取入口。
 *
 * <p>约束：微信渠道与微信支付配置只读取 sys_third_party_provider.config_json。
 */
public interface WechatRuntimeConfigService {

    String getMiniAppId();

    String getMiniAppSecret();

    String getMiniAdPid();

    String getMpAppId();

    String getMpAppSecret();

    String getMpToken();

    String getMpMatchRequestTemplateId();

    String getMpMatchRequestDetailUrl();

    String getAppAppId();

    String getWechatPayMchId();

    String getWechatPayApiKey();

    String getWechatPayNotifyUrl();
}
