package org.aileme.shejiao.app.runtime.thirdparty;

import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.PaymentRuntimeConfigService;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;
import org.aileme.shejiao.api.service.WechatRuntimeConfigService;

@Service
public class WechatRuntimeConfigServiceImpl implements WechatRuntimeConfigService {

    private static final String SERVICE_WECHAT_MINI = "wechat_mini";
    private static final String SERVICE_WECHAT_MP = "wechat_mp";
    private static final String SERVICE_WECHAT_APP = "wechat_app";

    private final ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService;
    private final PaymentRuntimeConfigService paymentRuntimeConfigService;

    public WechatRuntimeConfigServiceImpl(ThirdPartyRuntimeConfigService thirdPartyRuntimeConfigService,
                                          PaymentRuntimeConfigService paymentRuntimeConfigService) {
        this.thirdPartyRuntimeConfigService = thirdPartyRuntimeConfigService;
        this.paymentRuntimeConfigService = paymentRuntimeConfigService;
    }

    @Override
    public String getMiniAppId() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_MINI, "app_id", "appid");
    }

    @Override
    public String getMiniAppSecret() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_MINI, "app_secret", "secret");
    }

    @Override
    public String getMiniAdPid() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_MINI, "ad_pid", "wx_ad_pid", "adpid");
    }

    @Override
    public String getMpAppId() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_MP, "app_id", "appid");
    }

    @Override
    public String getMpAppSecret() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_MP, "app_secret", "secret");
    }

    @Override
    public String getMpToken() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_MP, "token");
    }

    @Override
    public String getMpMatchRequestTemplateId() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(
            SERVICE_WECHAT_MP,
            "match_request_template_id",
            "matchRequestTemplateId"
        );
    }

    @Override
    public String getMpMatchRequestDetailUrl() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(
            SERVICE_WECHAT_MP,
            "match_request_detail_url",
            "matchRequestDetailUrl"
        );
    }

    @Override
    public String getAppAppId() {
        return thirdPartyRuntimeConfigService.getCurrentConfig(SERVICE_WECHAT_APP, "app_id", "appid");
    }

    @Override
    public String getWechatPayMchId() {
        return paymentRuntimeConfigService.getWechatMchId();
    }

    @Override
    public String getWechatPayApiKey() {
        return paymentRuntimeConfigService.getWechatApiKey();
    }

    @Override
    public String getWechatPayNotifyUrl() {
        return paymentRuntimeConfigService.getWechatNotifyUrl();
    }
}
