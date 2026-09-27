package org.aileme.shejiao.app.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.SpringUtils;
import org.aileme.common.core.utils.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

import java.util.Map;

/**
 * 学历认证统一入口（策略式多厂商支持）。
 *
 * 根据第三方运行时配置中的 education provider 路由
 * 路由到不同的第三方学历认证服务（学信网、阿里云、百度云、自定义等）。
 *
 * 约定：外部服务返回的 JSON 中至少包含以下字段：
 *  - "层次"：如 "大专"、"本科" 等
 *  - "证件号码"：身份证号码
 *  - "学校名称"：毕业院校名称
 *
 * 具体对接某个云厂商时，由中间服务适配为上述字段即可。
 */
@Slf4j
public class EducationAuthApi {

    /**
     * 按配置的服务商查询学历信息
     *
     * @param code 学信网报告验证码或第三方定义的学历认证 code
     * @return JSON 解析后的 Map（实际是 JSONObject），失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> query(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }

        ThirdPartyRuntimeConfigService runtimeConfigService = SpringUtils.getBean(ThirdPartyRuntimeConfigService.class);
        String provider = runtimeConfigService.getProviderCode("education", "chsi", "education.provider");

        String urlConfigKey;
        switch (provider) {
            case "aliyun":
                urlConfigKey = "education.aliyun.url";
                break;
            case "baidu":
                urlConfigKey = "education.baidu.url";
                break;
            case "custom":
                urlConfigKey = "education.api.url";
                break;
            case "chsi":
            default:
                urlConfigKey = "education.chsi.url";
                break;
        }

        String apiUrl = runtimeConfigService.getConfig("education", provider, "url", "api_url", urlConfigKey, "education.api.url");
        if (StringUtils.isEmpty(apiUrl)) {
            log.warn("未配置学历认证接口地址: {} 或 education.api.url", urlConfigKey);
            return null;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("code", code)
                .build(true)
                .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            if (StringUtils.isEmpty(response)) {
                log.warn("学历认证接口返回空响应");
                return null;
            }
            JSONObject jsonObject = JSONObject.parseObject(response);
            return (Map<String, Object>) jsonObject;
        } catch (Exception e) {
            log.error("调用学历认证接口异常", e);
            return null;
        }
    }
}
