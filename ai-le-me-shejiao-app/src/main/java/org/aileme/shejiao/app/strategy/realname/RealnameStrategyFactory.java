package org.aileme.shejiao.app.strategy.realname;

import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.SpringUtils;
import org.aileme.common.core.utils.StringUtils;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

/**
 * 实名认证策略工厂
 *
 * 根据第三方运行时配置动态返回对应的策略实现。
 */
@Slf4j
public class RealnameStrategyFactory {

    /**
     * 获取实名认证策略实例
     *
     * @return 实名认证策略，如果配置不存在或无法识别则返回 null
     */
    public static RealnameStrategy getStrategy() {
        try {
            ThirdPartyRuntimeConfigService runtimeConfigService = SpringUtils.getBean(ThirdPartyRuntimeConfigService.class);
            String provider = runtimeConfigService.getProviderCode("realname", "aliyun", "realname.provider");

            log.info("实名认证使用服务商: {}", provider);

            switch (provider.toLowerCase()) {
                case "aliyun":
                    return new AliyunRealnameStrategy();
                case "tencent":
                    return new TencentRealnameStrategy();
                case "baidu":
                    // TODO: 如果后续需要接入百度云，在这里添加
                    log.warn("百度云实名认证策略暂未实现");
                    return null;
                default:
                    log.warn("不支持的实名认证服务商: {}", provider);
                    return null;
            }
        } catch (Exception e) {
            log.error("获取实名认证策略失败", e);
            return null;
        }
    }
}
