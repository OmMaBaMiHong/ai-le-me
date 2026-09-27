package org.aileme.shejiao.app.service.ai.video;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 豆包视频生成服务实现(Doubao SeeDance)
 * 
 * @author system
 * @date 2026-03-01
 */
@Slf4j
@Data
public class DoubaoVideoProvider implements AIVideoProvider {

    private String endpoint = "https://ark.cn-beijing.volces.com/api/v3";
    private String apiKey;
    private String model = "ep-20260318121955-mjq65";
    private Integer timeout = 30;

    private final HttpClient httpClient;

    public DoubaoVideoProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String createVideoTask(String prompt, List<String> images, Map<String, Object> params) {
        if (StringUtils.isBlank(apiKey)) {
            throw new RuntimeException("豆包视频API Key未配置");
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            // 构建content数组
            JSONArray content = new JSONArray();

            // 1. 添加文本提示词(包含参数)
            String fullPrompt = buildFullPrompt(prompt, params);
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", fullPrompt);
            content.add(textContent);

            // 2. 添加图片(如果提供了图片,使用第一张作为参考图)
            if (images != null && !images.isEmpty()) {
                String usableImageUrl = pickUsableImage(images);
                if (StringUtils.isNotBlank(usableImageUrl)) {
                    JSONObject imageContent = new JSONObject();
                    imageContent.put("type", "image_url");

                    JSONObject imageUrlObj = new JSONObject();
                    imageUrlObj.put("url", usableImageUrl);
                    imageContent.put("image_url", imageUrlObj);

                    content.add(imageContent);
                }
            }

            requestBody.put("content", content);

            // 发送HTTP请求
            String url = resolveApiBase() + "/contents/generations/tasks";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeout))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toJSONString()))
                    .build();

            log.info("豆包视频创建任务请求: {}", requestBody.toJSONString());

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("豆包视频创建任务响应: status={}, body={}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("豆包视频API调用失败: " + response.body());
            }

            // 解析响应
            JSONObject responseBody = JSON.parseObject(response.body());
            
            // 豆包返回的是 req_id 作为任务ID
            String taskId = responseBody.getString("req_id");
            if (StringUtils.isBlank(taskId)) {
                // 尝试其他可能的字段名
                taskId = responseBody.getString("task_id");
                if (StringUtils.isBlank(taskId)) {
                    taskId = responseBody.getString("id");
                }
            }

            if (StringUtils.isBlank(taskId)) {
                throw new RuntimeException("豆包视频任务创建失败,未返回任务ID: " + response.body());
            }

            return taskId;

        } catch (IOException | InterruptedException e) {
            log.error("豆包视频任务创建失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("豆包视频任务创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoTaskStatus queryTaskStatus(String taskId) {
        if (StringUtils.isBlank(apiKey)) {
            throw new RuntimeException("豆包视频API Key未配置");
        }

        try {
            // 查询任务状态
            String url = resolveApiBase() + "/contents/generations/tasks/" + taskId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeout))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("豆包视频查询任务: taskId={}, status={}, body={}", 
                    taskId, response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("豆包视频任务查询失败: " + response.body());
            }

            // 解析响应
            JSONObject responseBody = JSON.parseObject(response.body());
            return parseTaskStatus(responseBody);

        } catch (IOException | InterruptedException e) {
            log.error("豆包视频任务查询失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("豆包视频任务查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "doubao";
    }

    /**
     * 构建完整提示词(包含参数)
     */
    private String buildFullPrompt(String prompt, Map<String, Object> params) {
        StringBuilder fullPrompt = new StringBuilder(prompt);

        if (params != null) {
            // 添加时长参数
            Object duration = params.get("duration");
            if (duration != null) {
                fullPrompt.append(" --duration ").append(normalizeDuration(duration));
            }

            // 添加相机固定参数
            Object cameraFixed = params.get("camera_fixed");
            if (cameraFixed != null) {
                fullPrompt.append(" --camerafixed ").append(cameraFixed);
            }

            // 添加水印参数
            Object watermark = params.get("watermark");
            if (watermark != null) {
                fullPrompt.append(" --watermark ").append(watermark);
            }
        }

        return fullPrompt.toString();
    }

    /**
     * 解析任务状态响应
     */
    private VideoTaskStatus parseTaskStatus(JSONObject response) {
        VideoTaskStatus status = new VideoTaskStatus();

        // 解析状态字段
        String taskStatus = response.getString("status");
        if (StringUtils.isBlank(taskStatus)) {
            taskStatus = response.getString("task_status");
        }

        // 映射豆包状态到标准状态
        switch (taskStatus) {
            case "pending":
            case "submitted":
                status.setStatus("pending");
                status.setProgress(10);
                break;
            case "processing":
            case "running":
                status.setStatus("processing");
                // 尝试获取进度
                Integer progress = response.getInteger("progress");
                status.setProgress(progress != null ? progress : 50);
                break;
            case "succeeded":
            case "success":
            case "completed":
                status.setStatus("succeeded");
                status.setProgress(100);

                JSONObject content = response.getJSONObject("content");
                JSONObject output = response.getJSONObject("output");
                JSONObject payload = content != null ? content : output;

                if (payload != null) {
                    // 尝试多个可能的字段名
                    String videoUrl = payload.getString("video_url");
                    if (StringUtils.isBlank(videoUrl)) {
                        videoUrl = payload.getString("url");
                    }
                    if (StringUtils.isBlank(videoUrl)) {
                        JSONArray videos = payload.getJSONArray("videos");
                        if (videos != null && !videos.isEmpty()) {
                            videoUrl = videos.getJSONObject(0).getString("url");
                        }
                    }
                    status.setVideoUrl(videoUrl);

                    // 获取封面
                    String coverUrl = payload.getString("cover_url");
                    if (StringUtils.isBlank(coverUrl)) {
                        coverUrl = payload.getString("thumbnail");
                    }
                    status.setCoverUrl(coverUrl);
                }

                Integer duration = response.getInteger("duration");
                if (duration == null && payload != null) {
                    duration = payload.getInteger("duration");
                }
                status.setDuration(duration);
                break;
            case "failed":
            case "error":
                status.setStatus("failed");
                status.setProgress(0);
                
                // 获取错误信息
                String errorMsg = response.getString("error_message");
                if (StringUtils.isBlank(errorMsg)) {
                    errorMsg = response.getString("message");
                }
                if (StringUtils.isBlank(errorMsg)) {
                    JSONObject error = response.getJSONObject("error");
                    if (error != null) {
                        errorMsg = error.getString("message");
                    }
                }
                status.setErrorMessage(errorMsg);
                break;
            default:
                log.warn("未知的任务状态: {}", taskStatus);
                status.setStatus("pending");
                status.setProgress(0);
        }

        return status;
    }

    private String resolveApiBase() {
        String normalized = StringUtils.defaultIfBlank(endpoint, "https://ark.cn-beijing.volces.com/api/v3").trim();
        normalized = StringUtils.removeEnd(normalized, "/");
        normalized = StringUtils.removeEnd(normalized, "/contents/generations/tasks");
        return normalized;
    }

    private int normalizeDuration(Object durationValue) {
        int duration = 10;
        if (durationValue instanceof Number number) {
            duration = number.intValue();
        } else {
            try {
                duration = Integer.parseInt(String.valueOf(durationValue));
            } catch (NumberFormatException ignored) {
                duration = 10;
            }
        }
        return duration <= 5 ? 5 : 10;
    }

    private String pickUsableImage(List<String> images) {
        for (String imageUrl : images) {
            if (StringUtils.isBlank(imageUrl)) {
                continue;
            }
            if (isImageLargeEnough(imageUrl)) {
                return imageUrl;
            }
        }
        log.info("豆包视频未找到满足 >=300x300 的参考图，将退回纯文生视频");
        return null;
    }

    private boolean isImageLargeEnough(String imageUrl) {
        try {
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            if (image == null) {
                log.warn("读取参考图失败，无法识别图片内容: {}", imageUrl);
                return false;
            }
            boolean valid = image.getWidth() >= 300 && image.getHeight() >= 300;
            if (!valid) {
                log.info("跳过尺寸过小的参考图: {} ({}x{})", imageUrl, image.getWidth(), image.getHeight());
            }
            return valid;
        } catch (IOException e) {
            log.warn("校验参考图尺寸失败，跳过该图片: {}, error={}", imageUrl, e.getMessage());
            return false;
        }
    }
}
