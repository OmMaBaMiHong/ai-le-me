package org.aileme.shejiao.app.service.ai.image;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.shejiao.app.oss.factory.OSSFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Responses API 图片生成服务
 * 支持纯文生图，以及基于头像/生活照 URL 的参考图编辑生成。
 */
@Slf4j
@Data
public class OpenAIImageProvider implements AIImageProvider {

    private String endpoint = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "gpt-4.1";
    private String defaultSize = "1024x1536";
    private String defaultQuality = "high";
    private String defaultBackground = "auto";
    private String defaultInputFidelity = "high";
    private Integer defaultMaxImages = 3;
    private Integer timeout = 120;
    private Integer maxReferenceImages = 4;
    private String organizationId;
    private String projectId;

    private final HttpClient httpClient;

    public OpenAIImageProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public List<String> generateImages(String prompt, List<String> images, Map<String, Object> params) {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("OpenAI 图片 API Key 未配置");
        }
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("图片生成 prompt 不能为空");
        }

        List<String> referenceImages = normalizeImages(images);
        int desiredCount = Math.max(1, Math.min(4, intParam(params, "max_images", defaultMaxImages)));
        List<String> results = new ArrayList<>();
        for (int i = 0; i < desiredCount; i++) {
            String requestPrompt = decoratePrompt(prompt, i, desiredCount);
            results.addAll(generateSingle(requestPrompt, referenceImages, params));
        }
        return results;
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    private List<String> generateSingle(String prompt, List<String> referenceImages, Map<String, Object> params) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", stringParam(params, "model", model));
            body.put("input", buildInput(prompt, referenceImages));
            body.put("tools", buildTools(referenceImages, params));

            int requestTimeoutSeconds = Math.max(30, intParam(params, "timeout", timeout));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(resolveApiBase() + "/responses"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-Client-Request-Id", "openai-image-" + System.currentTimeMillis())
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()));
            if (StringUtils.isNotBlank(organizationId)) {
                requestBuilder.header("OpenAI-Organization", organizationId);
            }
            if (StringUtils.isNotBlank(projectId)) {
                requestBuilder.header("OpenAI-Project", projectId);
            }

            log.info("OpenAI 图片生成请求: {}", body.toJSONString());
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            log.info("OpenAI 图片生成响应: status={}, body={}", response.statusCode(), response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI 图片生成失败: HTTP " + response.statusCode() + " - " + response.body());
            }
            return parseAndPersistImages(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI 图片生成被中断: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI 图片生成失败: " + e.getMessage(), e);
        }
    }

    private JSONArray buildInput(String prompt, List<String> referenceImages) {
        JSONArray content = new JSONArray();
        content.add(JSON.parseObject("""
                {
                  "type": "input_text"
                }
                """));
        content.getJSONObject(0).put("text", prompt);
        for (String imageUrl : referenceImages) {
            JSONObject inputImage = new JSONObject();
            inputImage.put("type", "input_image");
            inputImage.put("image_url", imageUrl);
            content.add(inputImage);
        }

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", content);

        JSONArray input = new JSONArray();
        input.add(message);
        return input;
    }

    private JSONArray buildTools(List<String> referenceImages, Map<String, Object> params) {
        JSONObject tool = new JSONObject();
        tool.put("type", "image_generation");
        tool.put("size", normalizeSize(stringParam(params, "size", defaultSize), referenceImages));
        tool.put("quality", stringParam(params, "quality", defaultQuality));
        tool.put("background", stringParam(params, "background", defaultBackground));
        tool.put("input_fidelity", stringParam(params, "input_fidelity", defaultInputFidelity));
        tool.put("action", referenceImages.isEmpty() ? "generate" : "edit");

        JSONArray tools = new JSONArray();
        tools.add(tool);
        return tools;
    }

    private List<String> parseAndPersistImages(String responseBody) {
        JSONObject root = JSON.parseObject(responseBody);
        JSONArray output = root.getJSONArray("output");
        if (output == null || output.isEmpty()) {
            throw new IllegalStateException("OpenAI 图片生成未返回 output");
        }

        List<String> uploadedUrls = new ArrayList<>();
        for (int i = 0; i < output.size(); i++) {
            JSONObject item = output.getJSONObject(i);
            if (!StringUtils.equalsIgnoreCase(item.getString("type"), "image_generation_call")) {
                continue;
            }
            String imageBase64 = item.getString("result");
            if (StringUtils.isBlank(imageBase64)) {
                continue;
            }
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            String suffix = detectImageSuffix(imageBytes);
            String uploaded = OSSFactory.build().uploadSuffix(new ByteArrayInputStream(imageBytes), suffix);
            if (StringUtils.isNotBlank(uploaded)) {
                uploadedUrls.add(uploaded);
            }
        }

        if (uploadedUrls.isEmpty()) {
            throw new IllegalStateException("OpenAI 图片生成未返回可上传图片");
        }
        return uploadedUrls;
    }

    private List<String> normalizeImages(List<String> images) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        for (String image : images) {
            String trimmed = StringUtils.trimToEmpty(image);
            if (StringUtils.isBlank(trimmed)) {
                continue;
            }
            if (!StringUtils.startsWithIgnoreCase(trimmed, "http://")
                    && !StringUtils.startsWithIgnoreCase(trimmed, "https://")
                    && !StringUtils.startsWithIgnoreCase(trimmed, "data:image/")) {
                continue;
            }
            normalized.add(trimmed);
            if (normalized.size() >= Math.max(1, maxReferenceImages)) {
                break;
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeSize(String rawSize, List<String> referenceImages) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(rawSize));
        if (StringUtils.isBlank(normalized)) {
            return defaultSize;
        }
        if ("2k".equals(normalized) || "portrait".equals(normalized)) {
            return "1024x1536";
        }
        if ("landscape".equals(normalized)) {
            return "1536x1024";
        }
        if ("square".equals(normalized)) {
            return "1024x1024";
        }
        if (normalized.matches("\\d+x\\d+")) {
            return normalized;
        }
        return referenceImages.isEmpty() ? "1024x1024" : "1024x1536";
    }

    private String decoratePrompt(String prompt, int index, int total) {
        if (total <= 1) {
            return prompt;
        }
        return prompt + "\n补充要求：这是同一批次中的第 " + (index + 1) + " 张图，"
                + "请保持人物身份一致，但在场景、服装细节、动作、镜头距离和构图上与同批其他图片明显不同，避免同质化。";
    }

    private String detectImageSuffix(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 12) {
            return "png";
        }
        if ((imageBytes[0] & 0xFF) == 0x89
                && imageBytes[1] == 0x50
                && imageBytes[2] == 0x4E
                && imageBytes[3] == 0x47) {
            return "png";
        }
        if ((imageBytes[0] & 0xFF) == 0xFF
                && (imageBytes[1] & 0xFF) == 0xD8
                && (imageBytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (imageBytes[0] == 'R'
                && imageBytes[1] == 'I'
                && imageBytes[2] == 'F'
                && imageBytes[3] == 'F'
                && imageBytes[8] == 'W'
                && imageBytes[9] == 'E'
                && imageBytes[10] == 'B'
                && imageBytes[11] == 'P') {
            return "webp";
        }
        return "png";
    }

    private String resolveApiBase() {
        return StringUtils.removeEnd(StringUtils.defaultIfBlank(endpoint, "https://api.openai.com/v1"), "/");
    }

    private String stringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return defaultValue;
        }
        return String.valueOf(params.get(key));
    }

    private int intParam(Map<String, Object> params, String key, Integer defaultValue) {
        if (params == null || !params.containsKey(key) || params.get(key) == null) {
            return defaultValue == null ? 0 : defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(params.get(key)));
        } catch (NumberFormatException ignored) {
            return defaultValue == null ? 0 : defaultValue;
        }
    }
}
