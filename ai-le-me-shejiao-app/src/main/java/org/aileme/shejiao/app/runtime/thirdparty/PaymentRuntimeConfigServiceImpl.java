package org.aileme.shejiao.app.runtime.thirdparty;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PaymentRuntimeConfigService;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

/**
 * 支付运行时配置统一门面。
 */
@Service
public class PaymentRuntimeConfigServiceImpl implements PaymentRuntimeConfigService {

    private static final String SERVICE_PAYMENT = "payment";
    private static final String PROVIDER_WECHAT = "wechat";
    private static final String PROVIDER_ALIPAY = "alipay";

    private final ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService;

    public PaymentRuntimeConfigServiceImpl(ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService) {
        this.thirdPartyRuntimeConfigService = thirdPartyRuntimeConfigService;
    }

    @Override
    public String getWechatMchId() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_WECHAT, "mch_id");
    }

    @Override
    public String getWechatApiKey() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_WECHAT, "api_key");
    }

    @Override
    public String getWechatNotifyUrl() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_WECHAT, "notify_url");
    }

    @Override
    public String getWechatH5RedirectUrl() {
        return thirdPartyRuntimeConfigService.getConfig(
            SERVICE_PAYMENT,
            PROVIDER_WECHAT,
            "h5_redirect_url",
            "redirect_url",
            "return_url",
            "h5_domain"
        );
    }

    @Override
    public String getAlipayAppId() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_ALIPAY, "app_id", "appid");
    }

    @Override
    public String getAlipaySignType() {
        String signType = thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_ALIPAY, "sign_type");
        return StringUtils.defaultIfBlank(signType, "RSA2");
    }

    @Override
    public String getAlipayPrivateKey() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_ALIPAY, "private_key", "app_private_key");
    }

    @Override
    public String getAlipayPublicKey() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_ALIPAY, "alipay_public_key", "public_key");
    }

    @Override
    public String getAlipayNotifyUrl() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_ALIPAY, "notify_url");
    }

    @Override
    public String getAlipayReturnUrl() {
        return thirdPartyRuntimeConfigService.getConfig(SERVICE_PAYMENT, PROVIDER_ALIPAY, "return_url");
    }

    @Override
    public boolean isAlipayConfigured() {
        return StringUtils.isNoneBlank(
            getAlipayAppId(),
            getAlipayPrivateKey(),
            getAlipayPublicKey(),
            getAlipayNotifyUrl()
        );
    }
}
