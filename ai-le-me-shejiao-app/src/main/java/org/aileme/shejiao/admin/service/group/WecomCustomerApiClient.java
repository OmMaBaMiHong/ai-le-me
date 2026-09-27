package org.aileme.shejiao.admin.service.group;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.aileme.shejiao.common.exception.LinfengException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WecomCustomerApiClient {

    private static final String BASE_URL = "https://qyapi.weixin.qq.com/cgi-bin";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WecomCustomerApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String fetchAccessToken(String corpId, String corpSecret) {
        String url = BASE_URL + "/gettoken?corpid=" + corpId + "&corpsecret=" + corpSecret;
        Map<String, Object> response = get(url);
        Object accessToken = response.get("access_token");
        if (accessToken == null || String.valueOf(accessToken).isBlank()) {
            throw new LinfengException("企业微信 access_token 获取失败");
        }
        return String.valueOf(accessToken);
    }

    public Map<String, Object> getGroupChat(String accessToken, String chatId) {
        return post(
            BASE_URL + "/externalcontact/groupchat/get?access_token=" + accessToken,
            Map.of("chat_id", chatId)
        );
    }

    public Map<String, Object> addGroupMassMessage(String accessToken, Map<String, Object> payload) {
        return post(
            BASE_URL + "/externalcontact/add_msg_template?access_token=" + accessToken,
            payload
        );
    }

    private Map<String, Object> get(String url) {
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseResponse(response.getBody());
    }

    private Map<String, Object> post(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        return parseResponse(response.getBody());
    }

    private Map<String, Object> parseResponse(String body) {
        try {
            Map<String, Object> response = objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            ensureSuccess(response);
            return response;
        } catch (LinfengException e) {
            throw e;
        } catch (Exception e) {
            throw new LinfengException("企业微信接口响应解析失败");
        }
    }

    private void ensureSuccess(Map<String, Object> response) {
        Object errCode = response.get("errcode");
        int code = 0;
        if (errCode instanceof Number number) {
            code = number.intValue();
        } else if (errCode != null) {
            code = Integer.parseInt(String.valueOf(errCode));
        }
        if (code == 0) {
            return;
        }
        String errMsg = response.get("errmsg") == null ? "unknown" : String.valueOf(response.get("errmsg"));
        throw new LinfengException("企业微信接口调用失败：" + errMsg + "（" + code + "）");
    }
}
