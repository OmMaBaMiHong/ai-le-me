package org.aileme.shejiao.app.service.ai.video;

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
import java.util.List;
import java.util.Map;

/**
 * 即梦视频生成服务实现(Jimeng 3.0 Pro)
 * 
 * @author system
 * @date 2026-03-01
 */
@Slf4j
@Data
public class JimengVideoProvider implements AIVideoProvider {

    private String endpoint = "https://visual.volcengineapi.com";
    private String apiKey;
    private String accessKeyId;
    private String secretAccessKey;
    private Integer timeout = 30;
    
    // 固定参数
    private static final String REQ_KEY = "jimeng_ti2v_v30_pro";
    private static final String ACTION_SUBMIT = "CVSync2AsyncSubmitTask";
    private static final String ACTION_QUERY = "CVSync2AsyncGetResult";
    private static final String VERSION = "2022-08-31";
    private static final String REGION = "cn-north-1";
    private static final String SERVICE = "cv";

    private final HttpClient httpClient;

    public JimengVideoProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String createVideoTask(String prompt, List<String> images, Map<String, Object> params) {
        if (StringUtils.isBlank(accessKeyId) || StringUtils.isBlank(secretAccessKey)) {
            throw new RuntimeException("即梦视频AK/SK未配置");
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("req_key", REQ_KEY);
            requestBody.put("prompt", prompt);
            
            // 添加图片(如果有)
            if (images != null && !images.isEmpty()) {
                JSONArray imageUrls = new JSONArray();
                for (String imageUrl : images) {
                    if (StringUtils.isNotBlank(imageUrl)) {
                        imageUrls.add(imageUrl);
                        break; // 即梦只支持一张首帧图片
                    }
                }
                if (!imageUrls.isEmpty()) {
                    requestBody.put("image_urls", imageUrls);
                }
            }

            // 设置随机种子
            requestBody.put("seed", -1);

            // 设置帧数(frames = 24 * n + 1, 支持5s或10s)
            Integer duration = params != null ? (Integer) params.get("duration") : 5;
            int frames = duration != null && duration >= 10 ? 241 : 121; // 10秒=241帧, 5秒=121帧
            requestBody.put("frames", frames);

            // 设置宽高比
            String aspectRatio = params != null ? (String) params.get("aspect_ratio") : "16:9";
            if (StringUtils.isBlank(aspectRatio)) {
                aspectRatio = "16:9";
            }
            // 转换格式: 9:16 -> 9:16 (即梦支持的格式)
            requestBody.put("aspect_ratio", aspectRatio);

            // 构建URL
            String url = endpoint + "?Action=" + ACTION_SUBMIT + "&Version=" + VERSION;

            // 发送HTTP请求(需要使用火山引擎签名)
            HttpRequest request = buildSignedRequest(url, requestBody.toJSONString(), "POST");

            log.info("即梦视频创建任务请求: {}", requestBody.toJSONString());

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("即梦视频创建任务响应: status={}, body={}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("即梦视频API调用失败: " + response.body());
            }

            // 解析响应
            JSONObject responseBody = JSON.parseObject(response.body());
            
            // 检查状态码
            Integer code = responseBody.getInteger("code");
            if (code == null || code != 10000) {
                String message = responseBody.getString("message");
                throw new RuntimeException("即梦视频任务创建失败: " + message);
            }

            // 获取任务ID
            JSONObject data = responseBody.getJSONObject("data");
            if (data == null) {
                throw new RuntimeException("即梦视频任务创建失败,未返回data");
            }

            String taskId = data.getString("task_id");
            if (StringUtils.isBlank(taskId)) {
                throw new RuntimeException("即梦视频任务创建失败,未返回任务ID");
            }

            return taskId;

        } catch (IOException | InterruptedException e) {
            log.error("即梦视频任务创建失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("即梦视频任务创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoTaskStatus queryTaskStatus(String taskId) {
        if (StringUtils.isBlank(accessKeyId) || StringUtils.isBlank(secretAccessKey)) {
            throw new RuntimeException("即梦视频AK/SK未配置");
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("req_key", REQ_KEY);
            requestBody.put("task_id", taskId);

            // 构建URL
            String url = endpoint + "?Action=" + ACTION_QUERY + "&Version=" + VERSION;

            // 发送HTTP请求
            HttpRequest request = buildSignedRequest(url, requestBody.toJSONString(), "POST");

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("即梦视频查询任务: taskId={}, status={}, body={}", 
                    taskId, response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("即梦视频任务查询失败: " + response.body());
            }

            // 解析响应
            JSONObject responseBody = JSON.parseObject(response.body());
            return parseTaskStatus(responseBody);

        } catch (IOException | InterruptedException e) {
            log.error("即梦视频任务查询失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("即梦视频任务查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "jimeng";
    }

    /**
     * 构建带签名的HTTP请求
     * 注意: 这里需要实现火山引擎的签名算法
     * 参考: https://www.volcengine.com/docs/6369/67268
     */
    private HttpRequest buildSignedRequest(String url, String body, String method) {
        // TODO: 实现火山引擎签名算法
        // 这里暂时使用简单的Bearer Token方式(如果火山引擎支持)
        // 实际应该使用AK/SK签名
        
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout));

        // 如果有apiKey,使用Bearer认证
        if (StringUtils.isNotBlank(apiKey)) {
            builder.header("Authorization", "Bearer " + apiKey);
        } else {
            // 使用AK/SK签名(需要实现签名算法)
            // 这里先添加基本的header
            builder.header("X-Access-Key-Id", accessKeyId);
            // TODO: 添加签名相关的header
            log.warn("即梦视频Provider使用AK/SK认证,需要实现签名算法");
        }

        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }

        return builder.build();
    }

    /**
     * 解析任务状态响应
     */
    private VideoTaskStatus parseTaskStatus(JSONObject response) {
        VideoTaskStatus status = new VideoTaskStatus();

        // 检查状态码
        Integer code = response.getInteger("code");
        if (code == null || code != 10000) {
            // 任务失败或其他错误
            String message = response.getString("message");
            status.setStatus("failed");
            status.setProgress(0);
            status.setErrorMessage(message != null ? message : "查询任务失败");
            return status;
        }

        // 解析data
        JSONObject data = response.getJSONObject("data");
        if (data == null) {
            status.setStatus("pending");
            status.setProgress(0);
            return status;
        }

        // 解析任务状态
        String taskStatus = data.getString("status");
        if (StringUtils.isBlank(taskStatus)) {
            status.setStatus("pending");
            status.setProgress(0);
            return status;
        }

        // 映射即梦状态到标准状态
        switch (taskStatus) {
            case "in_queue":
                status.setStatus("pending");
                status.setProgress(10);
                break;
                
            case "generating":
                status.setStatus("processing");
                status.setProgress(50);
                break;
                
            case "done":
                // 处理完成,检查是否成功
                String videoUrl = data.getString("video_url");
                if (StringUtils.isNotBlank(videoUrl)) {
                    status.setStatus("succeeded");
                    status.setProgress(100);
                    status.setVideoUrl(videoUrl);
                    
                    // 即梦没有返回封面和时长,可以从URL推断或设置默认值
                    // status.setCoverUrl(coverUrl);
                    // status.setDuration(duration);
                } else {
                    // done但没有视频URL,认为是失败
                    status.setStatus("failed");
                    status.setProgress(0);
                    String message = response.getString("message");
                    status.setErrorMessage(message != null ? message : "视频生成失败");
                }
                break;
                
            case "not_found":
                status.setStatus("failed");
                status.setProgress(0);
                status.setErrorMessage("任务未找到");
                break;
                
            case "expired":
                status.setStatus("failed");
                status.setProgress(0);
                status.setErrorMessage("任务已过期");
                break;
                
            default:
                log.warn("未知的任务状态: {}", taskStatus);
                status.setStatus("pending");
                status.setProgress(0);
        }

        return status;
    }
}
