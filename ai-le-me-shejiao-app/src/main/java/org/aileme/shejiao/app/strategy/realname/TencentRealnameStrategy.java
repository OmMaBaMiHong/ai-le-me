package org.aileme.shejiao.app.strategy.realname;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aileme.common.core.utils.SpringUtils;
import org.aileme.common.core.utils.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.aileme.shejiao.api.service.ThirdPartyRuntimeConfigService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 腾讯云实名认证策略实现（身份二要素核验）
 *
 * 使用腾讯云人脸核身 - 身份信息认证接口（IdCardVerification）
 * 文档：https://cloud.tencent.com/document/product/1007/33188
 *
 * 优先读取：
 * - sys_third_party_provider(service_type=realname, provider_code=tencent).config_json
 *
 * 迁移期兼容旧键：
 * - realname.tencent.secretId
 * - realname.tencent.secretKey
 * - realname.tencent.region
 *
 * 注意：当前使用 REST API 方式调用，需要自己签名。
 * 正式环境建议使用腾讯云官方 Java SDK，更简单稳定。
 */
@Slf4j
public class TencentRealnameStrategy implements RealnameStrategy {

    private static final String SERVICE = "faceid";
    private static final String VERSION = "2018-03-01";
    private static final String ACTION = "IdCardVerification";
    private static final String HOST = "faceid.tencentcloudapi.com";

    @Override
    public JSONObject verify(String name, String idCard) {
        JSONObject result = new JSONObject();
        result.put("Result", "FAIL");
        result.put("Description", "未知错误");

        try {
            ThirdPartyRuntimeConfigService runtimeConfigService = SpringUtils.getBean(ThirdPartyRuntimeConfigService.class);
            String secretId = runtimeConfigService.getConfig(
                "realname", "tencent", "secret_id", "secretId", "realname.tencent.secretId"
            );
            String secretKey = runtimeConfigService.getConfig(
                "realname", "tencent", "secret_key", "secretKey", "realname.tencent.secretKey"
            );
            String region = runtimeConfigService.getConfig(
                "realname", "tencent", "region", "realname.tencent.region"
            );

            if (StringUtils.isEmpty(secretId) || StringUtils.isEmpty(secretKey)) {
                log.warn("腾讯云实名认证配置缺失: secretId 或 secretKey");
                result.put("Description", "腾讯云实名认证配置缺失");
                return result;
            }

            if (StringUtils.isEmpty(region)) {
                region = "ap-guangzhou";
            }

            // 构造请求体
            JSONObject payload = new JSONObject();
            payload.put("Name", name);
            payload.put("IdCard", idCard);
            String payloadStr = payload.toJSONString();

            // 生成签名
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            String authorization = generateAuthorization(secretId, secretKey, HOST, 
                payloadStr, timestamp, date, region);

            // 调用接口
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Host", HOST);
            headers.set("X-TC-Action", ACTION);
            headers.set("X-TC-Version", VERSION);
            headers.set("X-TC-Timestamp", timestamp);
            headers.set("X-TC-Region", region);
            headers.set("Authorization", authorization);

            org.springframework.http.HttpEntity<String> request = 
                new org.springframework.http.HttpEntity<>(payloadStr, headers);

            String response = restTemplate.postForObject(
                "https://" + HOST, request, String.class);

            if (StringUtils.isNotEmpty(response)) {
                JSONObject resp = JSONObject.parseObject(response);
                JSONObject responseObj = resp.getJSONObject("Response");

                if (responseObj != null) {
                    String res = responseObj.getString("Result");
                    String desc = responseObj.getString("Description");

                    if ("0".equals(res)) {
                        result.put("Result", "PASS");
                        result.put("Description", desc != null ? desc : "姓名和身份证号一致");
                        result.put("Name", desensitizeName(name));
                        result.put("IdCard", desensitizeIdCard(idCard));
                    } else {
                        result.put("Result", "FAIL");
                        result.put("Description", desc != null ? desc : "姓名和身份证号不一致");
                    }
                } else {
                    result.put("Description", "腾讯云接口返回格式异常");
                }
            } else {
                result.put("Description", "腾讯云接口返回为空");
            }

        } catch (Exception e) {
            log.error("腾讯云实名认证调用异常", e);
            result.put("Description", "腾讯云实名认证调用异常: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String getProviderName() {
        return "tencent";
    }

    /**
     * 生成腾讯云 API v3 签名
     */
    private String generateAuthorization(String secretId, String secretKey, String host,
                                         String payload, String timestamp, String date, String region) 
                                         throws Exception {
        // 1. 拼接规范请求串
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json\nhost:" + host + "\n";
        String signedHeaders = "content-type;host";
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = httpRequestMethod + "\n" + canonicalUri + "\n" + 
            canonicalQueryString + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + 
            hashedRequestPayload;

        // 2. 拼接待签名字符串
        String algorithm = "TC3-HMAC-SHA256";
        String credentialScope = date + "/" + SERVICE + "/" + "tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + 
            hashedCanonicalRequest;

        // 3. 计算签名
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, SERVICE);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = bytesToHex(hmac256(secretSigning, stringToSign));

        // 4. 拼接 Authorization
        return algorithm + " " + "Credential=" + secretId + "/" + credentialScope + ", " +
            "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }

    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(d);
    }

    private byte[] hmac256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
