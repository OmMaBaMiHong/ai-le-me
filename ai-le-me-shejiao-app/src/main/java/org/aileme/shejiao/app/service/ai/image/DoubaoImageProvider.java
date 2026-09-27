package org.aileme.shejiao.app.service.ai.image;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 豆包 Seedream 图片生成服务
 */
@Slf4j
@Data
public class DoubaoImageProvider implements AIImageProvider {

    private static final ConcurrentMap<String, ModelCircuitState> CIRCUIT_STATES = new ConcurrentHashMap<>();

    private String endpoint = "https://ark.cn-beijing.volces.com/api/v3";
    private String apiKey;
    private String model = "ep-20260323151528-8khnp";
    private String fallbackModel = "doubao-seedream-4-5-251128";
    private String fallbackModels = "doubao-seedream-4-5-251128,doubao-seedream-4-0-250828,doubao-seedream-3-0-t2i-250415";
    private String defaultSize = "2K";
    private String defaultResponseFormat = "url";
    private Boolean defaultWatermark = Boolean.TRUE;
    private Integer defaultMaxImages = 3;
    private Integer timeout = 60;
    private Boolean autoFailover = Boolean.TRUE;
    private Integer circuitFailureThreshold = 3;
    private Integer circuitOpenSeconds = 180;

    private final HttpClient httpClient;

    public DoubaoImageProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public List<String> generateImages(String prompt, List<String> images, Map<String, Object> params) {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalStateException("豆包图片 API Key 未配置");
        }
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("图片生成 prompt 不能为空");
        }
        List<String> candidates = buildCandidateModels(params, images);
        Exception lastError = null;
        for (String candidate : candidates) {
            if (isCircuitOpen(candidate) && candidates.size() > 1) {
                log.warn("豆包图片模型 {} 熔断中，跳过本次调用", candidate);
                continue;
            }
            try {
                List<String> result = generateWithImageFallback(candidate, prompt, images, params);
                onModelSuccess(candidate);
                return result;
            } catch (Exception ex) {
                lastError = ex;
                onModelFailure(candidate, ex);
                log.warn("豆包图片模型 {} 调用失败: {}", candidate, ex.getMessage());
                if (!Boolean.TRUE.equals(autoFailover) || !shouldContinueFailover(candidate, candidates)) {
                    break;
                }
            }
        }
        if (lastError instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(lastError == null ? "豆包图片生成失败" : lastError.getMessage(), lastError);
    }

    private List<String> generateWithImageFallback(String currentModel,
                                                   String prompt,
                                                   List<String> images,
                                                   Map<String, Object> params) {
        List<String> normalizedImages = normalizeImages(images);
        try {
            return doGenerate(currentModel, prompt, normalizedImages, params);
        } catch (Exception ex) {
            if (!shouldRetryWithSingleImage(ex, normalizedImages)) {
                throw ex;
            }
            log.warn("豆包图片模型 {} 多图参考触发敏感拦截，降级为单图逐张重试", currentModel);
            RuntimeException lastSingleError = toRuntimeException(ex);
            for (String image : normalizedImages) {
                try {
                    return doGenerate(currentModel, prompt, List.of(image), buildSingleImageRetryParams(params));
                } catch (Exception singleEx) {
                    lastSingleError = toRuntimeException(singleEx);
                    log.warn("豆包图片模型 {} 单图重试失败: {}", currentModel, singleEx.getMessage());
                    if (!isSensitiveInputImageError(singleEx)) {
                        throw lastSingleError;
                    }
                }
            }
            throw lastSingleError;
        }
    }

    @Override
    public String getProviderName() {
        return "doubao";
    }

    private List<String> doGenerate(String currentModel, String prompt, List<String> images, Map<String, Object> params) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", currentModel);
            body.put("prompt", prompt);
            body.put("response_format", stringParam(params, "response_format", defaultResponseFormat));
            body.put("size", stringParam(params, "size", defaultSize));
            body.put("stream", false);
            body.put("watermark", boolParam(params, "watermark", defaultWatermark));

            List<String> referenceImages = normalizeImages(images);
            if (!referenceImages.isEmpty()) {
                if (referenceImages.size() == 1) {
                    body.put("image", referenceImages.get(0));
                } else {
                    body.put("image", referenceImages);
                }
                body.put("sequential_image_generation", stringParam(params, "sequential_image_generation", "auto"));
                body.put("max_images", intParam(params, "max_images", defaultMaxImages));
            } else {
                body.put("sequential_image_generation", stringParam(params, "sequential_image_generation", "disabled"));
            }

            int requestTimeoutSeconds = Math.max(30, intParam(params, "timeout", timeout));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveApiBase() + "/images/generations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                    .build();

            log.info("豆包图片生成请求: {}", body.toJSONString());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("豆包图片生成响应: status={}, body={}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("豆包图片生成失败: HTTP " + response.statusCode() + " - " + response.body());
            }
            return parseImageUrls(response.body());
        } catch (IOException | InterruptedException e) {
            log.error("豆包图片生成失败", e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("豆包图片生成失败: " + e.getMessage(), e);
        }
    }

    private List<String> buildCandidateModels(Map<String, Object> params, List<String> images) {
        Set<String> candidates = new LinkedHashSet<>();
        String primary = stringParam(params, "model", model);
        if (StringUtils.isNotBlank(primary)) {
            candidates.add(primary.trim());
        }
        String fallback = stringParam(params, "fallback_model", fallbackModel);
        if (Boolean.TRUE.equals(autoFailover) && StringUtils.isNotBlank(fallback) && !StringUtils.equalsIgnoreCase(primary, fallback)) {
            candidates.add(fallback.trim());
        }
        if (Boolean.TRUE.equals(autoFailover)) {
            for (String item : parseModelList(stringParam(params, "fallback_models", fallbackModels))) {
                if (StringUtils.isNotBlank(item) && !StringUtils.equalsIgnoreCase(primary, item)) {
                    candidates.add(item.trim());
                }
            }
        }
        boolean hasReferenceImages = images != null && !normalizeImages(images).isEmpty();
        List<String> filtered = new ArrayList<>();
        for (String candidate : candidates) {
            if (hasReferenceImages && isTextToImageOnlyModel(candidate)) {
                continue;
            }
            filtered.add(candidate);
        }
        if (filtered.isEmpty() && StringUtils.isNotBlank(primary)) {
            filtered.add(primary.trim());
        }
        return filtered;
    }

    private boolean shouldContinueFailover(String currentModel, List<String> candidates) {
        return candidates.indexOf(currentModel) < candidates.size() - 1;
    }

    private boolean shouldRetryWithSingleImage(Exception ex, List<String> normalizedImages) {
        return normalizedImages != null
                && normalizedImages.size() > 1
                && isSensitiveInputImageError(ex);
    }

    private boolean isSensitiveInputImageError(Exception ex) {
        String message = ex == null ? "" : StringUtils.defaultString(ex.getMessage());
        return StringUtils.containsIgnoreCase(message, "InputImageSensitiveContentDetected");
    }

    private RuntimeException toRuntimeException(Exception ex) {
        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(StringUtils.defaultIfBlank(ex.getMessage(), "豆包图片生成失败"), ex);
    }

    private Map<String, Object> buildSingleImageRetryParams(Map<String, Object> params) {
        Map<String, Object> retryParams = new LinkedHashMap<>();
        if (params != null) {
            retryParams.putAll(params);
        }
        int currentMaxImages = intParam(params, "max_images", defaultMaxImages);
        retryParams.put("max_images", Math.max(1, Math.min(currentMaxImages, 2)));
        retryParams.put("timeout", Math.max(timeout, 120));
        return retryParams;
    }

    private boolean isTextToImageOnlyModel(String currentModel) {
        return StringUtils.containsIgnoreCase(StringUtils.defaultString(currentModel), "t2i");
    }

    private boolean isCircuitOpen(String currentModel) {
        ModelCircuitState state = CIRCUIT_STATES.get(currentModel);
        return state != null && state.isOpen();
    }

    private void onModelSuccess(String currentModel) {
        CIRCUIT_STATES.computeIfAbsent(currentModel, key -> new ModelCircuitState()).reset();
    }

    private void onModelFailure(String currentModel, Exception ex) {
        ModelCircuitState state = CIRCUIT_STATES.computeIfAbsent(currentModel, key -> new ModelCircuitState());
        int failures = state.failureCount.incrementAndGet();
        if (failures >= Math.max(1, circuitFailureThreshold)) {
            long openUntil = System.currentTimeMillis() + Math.max(30, circuitOpenSeconds) * 1000L;
            state.openUntilMillis = openUntil;
            state.failureCount.set(0);
            log.warn("豆包图片模型 {} 已熔断至 {}", currentModel, openUntil);
        }
    }

    private List<String> parseImageUrls(String rawBody) {
        JSONObject body = JSON.parseObject(rawBody);
        List<String> urls = new ArrayList<>();
        JSONArray data = body.getJSONArray("data");
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                Object item = data.get(i);
                if (item instanceof JSONObject object) {
                    String url = object.getString("url");
                    if (StringUtils.isNotBlank(url)) {
                        urls.add(url.trim());
                    }
                } else if (item instanceof String value && StringUtils.isNotBlank(value)) {
                    urls.add(value.trim());
                }
            }
        }
        if (urls.isEmpty()) {
            throw new IllegalStateException("豆包图片生成未返回图片 URL: " + rawBody);
        }
        return urls;
    }

    private List<String> normalizeImages(List<String> images) {
        List<String> normalized = new ArrayList<>();
        if (images == null) {
            return normalized;
        }
        for (String image : images) {
            if (StringUtils.isNotBlank(image) && normalized.size() < 14) {
                normalized.add(image.trim());
            }
        }
        return normalized;
    }

    private String stringParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(params.get(key)).trim();
        return StringUtils.defaultIfBlank(value, defaultValue);
    }

    private Integer intParam(Map<String, Object> params, String key, Integer defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(params.get(key)).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private Boolean boolParam(Map<String, Object> params, String key, Boolean defaultValue) {
        if (params == null || params.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(params.get(key)).trim();
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        return defaultValue;
    }

    private String resolveApiBase() {
        String normalized = StringUtils.removeEnd(StringUtils.defaultIfBlank(endpoint, "https://ark.cn-beijing.volces.com/api/v3").trim(), "/");
        normalized = StringUtils.removeEnd(normalized, "/images/generations");
        normalized = StringUtils.removeEnd(normalized, "/contents/generations/tasks");
        if (normalized.endsWith("/api/v3")) {
            return normalized;
        }
        if (normalized.contains("/api/v3/")) {
            return normalized.substring(0, normalized.indexOf("/api/v3/") + "/api/v3".length());
        }
        return normalized + "/api/v3";
    }

    private List<String> parseModelList(String raw) {
        List<String> models = new ArrayList<>();
        if (StringUtils.isBlank(raw)) {
            return models;
        }
        String source = raw.trim();
        if (source.startsWith("[")) {
            JSONArray array = JSON.parseArray(source);
            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    String item = array.getString(i);
                    if (StringUtils.isNotBlank(item)) {
                        models.add(item.trim());
                    }
                }
            }
            return models;
        }
        for (String item : source.split("\\s*,\\s*")) {
            if (StringUtils.isNotBlank(item)) {
                models.add(item.trim());
            }
        }
        return models;
    }

    private static class ModelCircuitState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile long openUntilMillis = 0L;

        private boolean isOpen() {
            if (openUntilMillis <= System.currentTimeMillis()) {
                openUntilMillis = 0L;
                return false;
            }
            return true;
        }

        private void reset() {
            failureCount.set(0);
            openUntilMillis = 0L;
        }
    }
}
