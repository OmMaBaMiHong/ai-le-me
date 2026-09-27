package org.aileme.shejiao.common.utils;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AlipaySignUtils {

    private static final String RSA2_ALGORITHM = "SHA256withRSA";

    private AlipaySignUtils() {
    }

    public static String sign(Map<String, String> params, String privateKey) {
        try {
            String content = buildSignContent(params);
            Signature signature = Signature.getInstance(RSA2_ALGORITHM);
            signature.initSign(loadPrivateKey(privateKey));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败", e);
        }
    }

    public static boolean verify(Map<String, String> params, String publicKey, String sign) {
        try {
            String content = buildSignContent(params);
            Signature signature = Signature.getInstance(RSA2_ALGORITHM);
            signature.initVerify(loadPublicKey(publicKey));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    public static String buildSignContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder content = new StringBuilder();
        for (String key : keys) {
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            String value = params.get(key);
            if (StrUtil.isBlank(value)) {
                continue;
            }
            if (!content.isEmpty()) {
                content.append("&");
            }
            content.append(key).append("=").append(value);
        }
        return content.toString();
    }

    private static RSAPrivateKey loadPrivateKey(String privateKey) throws Exception {
        String normalized = normalizeKey(privateKey);
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    private static PublicKey loadPublicKey(String publicKey) throws Exception {
        String normalized = normalizeKey(publicKey);
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private static String normalizeKey(String key) {
        return StrUtil.blankToDefault(key, "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
