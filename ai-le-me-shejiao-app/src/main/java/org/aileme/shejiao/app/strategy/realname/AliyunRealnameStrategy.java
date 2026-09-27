package org.aileme.shejiao.app.strategy.realname;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.SpringUtils;
import org.aileme.common.core.utils.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 阿里云实名认证策略实现（身份二要素核验）
 *
 * 使用阿里云实人认证 - 身份信息认证接口
 * 文档：https://help.aliyun.com/document_detail/466849.html
 *
 * 优先读取：
 * - sys_third_party_provider(service_type=realname, provider_code=aliyun).config_json
 *
 * 迁移期兼容旧键：
 * - realname.aliyun.accessKeyId
 * - realname.aliyun.accessKeySecret
 *
 * 注意：当前使用 REST API 方式调用，需要自己签名。
 * 正式环境建议使用阿里云官方 Java SDK，更简单稳定。
 */
@Slf4j
public class AliyunRealnameStrategy implements RealnameStrategy {

    @Override
    public JSONObject verify(String name, String idCard) {
        JSONObject result = new JSONObject();
        result.put("Result", "FAIL");
        result.put("Description", "未知错误");

        try {
            ThirdPartyRuntimeConfigService runtimeConfigService = SpringUtils.getBean(ThirdPartyRuntimeConfigService.class);
            String accessKeyId = runtimeConfigService.getConfig(
                "realname", "aliyun", "access_key_id", "accessKeyId", "realname.aliyun.accessKeyId"
            );
            String accessKeySecret = runtimeConfigService.getConfig(
                "realname", "aliyun", "access_key_secret", "accessKeySecret", "realname.aliyun.accessKeySecret"
            );

            if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)) {
                log.warn("阿里云实名认证配置缺失: accessKeyId 或 accessKeySecret");
                result.put("Description", "阿里云实名认证配置缺失");
                return result;
            }

            // 构造请求参数
            Map<String, String> params = new TreeMap<>();
            params.put("Action", "VerifyMaterial");
            params.put("Version", "2019-03-07");
            params.put("IdCardName", name);
            params.put("IdCardNumber", idCard);
            params.put("AccessKeyId", accessKeyId);
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("Timestamp", getISO8601Timestamp());
            params.put("Format", "JSON");

            // 生成签名
            String signature = generateSignature(params, accessKeySecret);
            params.put("Signature", signature);

            // 构造 URL
            StringBuilder url = new StringBuilder("https://cloudauth.aliyuncs.com/?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                url.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                    .append("&");
            }
            url.deleteCharAt(url.length() - 1);

            // 调用接口
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url.toString(), String.class);

            if (StringUtils.isNotEmpty(response)) {
                JSONObject resp = JSONObject.parseObject(response);
                String verifyResult = resp.getString("VerifyResult");

                if ("PASS".equals(verifyResult)) {
                    result.put("Result", "PASS");
                    result.put("Description", "姓名和身份证号一致");
                    result.put("Name", desensitizeName(name));
                    result.put("IdCard", desensitizeIdCard(idCard));
                } else {
                    result.put("Result", "FAIL");
                    result.put("Description", "姓名和身份证号不一致");
                }
            } else {
                result.put("Description", "阿里云接口返回为空");
            }

        } catch (Exception e) {
            log.error("阿里云实名认证调用异常", e);
            result.put("Description", "阿里云实名认证调用异常: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }

    /**
     * 生成阿里云 API 签名
     */
    private String generateSignature(Map<String, String> params, String secret) throws Exception {
        StringBuilder canonicalizedQueryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"Signature".equals(entry.getKey())) {
                canonicalizedQueryString.append("&")
                    .append(percentEncode(entry.getKey()))
                    .append("=")
                    .append(percentEncode(entry.getValue()));
            }
        }
        String stringToSign = "GET&%2F&" + percentEncode(canonicalizedQueryString.substring(1));

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((secret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    private String percentEncode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~");
    }

    private String getISO8601Timestamp() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }

    /**
     * 姓名脱敏：张三 -> 张*
     */
    private String desensitizeName(String name) {
        if (StringUtils.isEmpty(name) || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 身份证号脱敏：440112199001011234 -> 440112********1234
     */
    private String desensitizeIdCard(String idCard) {
        if (StringUtils.isEmpty(idCard) || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}
