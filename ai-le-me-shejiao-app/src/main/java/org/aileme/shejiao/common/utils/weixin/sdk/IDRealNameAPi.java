package org.aileme.shejiao.common.utils.weixin.sdk;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.StringUtils;
import org.aileme.shejiao.app.strategy.realname.RealnameStrategy;
import org.aileme.shejiao.app.strategy.realname.RealnameStrategyFactory;

/**
 * 实名认证接口封装（策略模式）
 *
 * 根据 sys_config 中的 realname.provider 配置，
 * 动态调用阿里云/腾讯云/百度云等不同云服务商的实名认证接口。
 *
 * 支持的服务商：
 * - aliyun：阿里云实人认证
 * - tencent：腾讯云人脸核身
 * - baidu：百度云（待实现）
 */
@Slf4j
public class IDRealNameAPi {

    /**
     * 调用实名认证接口（身份二要素）
     *
     * @param name   真实姓名
     * @param idCard 身份证号
     * @return 三方返回的 JSON 字符串，失败返回 null
     */
    public static String verify(String name, String idCard) {
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(idCard)) {
            log.warn("实名认证参数缺失: name={}, idCard={}", name, idCard);
            return null;
        }

        try {
            // 获取策略实例
            RealnameStrategy strategy = RealnameStrategyFactory.getStrategy();
            if (strategy == null) {
                log.warn("未配置实名认证服务商或服务商不支持");
                return null;
            }

            // 调用策略执行认证
            log.info("调用实名认证, 服务商: {}, 姓名: {}", strategy.getProviderName(), name);
            JSONObject result = strategy.verify(name, idCard);

            if (result != null) {
                return result.toJSONString();
            } else {
                log.warn("实名认证策略返回为 null");
                return null;
            }
        } catch (Exception e) {
            log.error("实名认证调用异常", e);
            return null;
        }
    }

    /**
     * 兼容旧版本接口（仅传身份证正面图片地址）
     *
     * @param idCardFront 身份证正面图片地址（一般为 OSS URL）
     * @return 三方返回的原始 JSON 字符串，失败返回 null
     * @deprecated 建议使用 verify(String name, String idCard) 方法
     */
    @Deprecated
    public static String idCard(String idCardFront) {
        log.warn("调用了已废弃的 idCard 方法，请使用 verify(name, idCard)");
        // 这里无法从图片获取姓名和身份证号，返回 null
        return null;
    }
}
