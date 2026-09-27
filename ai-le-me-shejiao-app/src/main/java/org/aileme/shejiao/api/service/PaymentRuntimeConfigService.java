package org.aileme.shejiao.api.service;

/**
 * 支付运行时配置统一读取入口。
 *
 * <p>约束：支付渠道密钥、商户参数等只读取 sys_third_party_provider.config_json。</p>
 */
public interface PaymentRuntimeConfigService {

    String getWechatMchId();

    String getWechatApiKey();

    String getWechatNotifyUrl();

    String getWechatH5RedirectUrl();

    String getAlipayAppId();

    String getAlipaySignType();

    String getAlipayPrivateKey();

    String getAlipayPublicKey();

    String getAlipayNotifyUrl();

    String getAlipayReturnUrl();

    boolean isAlipayConfigured();
}
