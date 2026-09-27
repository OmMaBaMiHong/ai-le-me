package org.aileme.shejiao.app.service.ai.video;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock视频生成服务(用于测试)
 * 
 * @author system
 * @date 2026-03-01
 */
@Slf4j
public class MockVideoProvider implements AIVideoProvider {

    @Override
    public String createVideoTask(String prompt, List<String> images, Map<String, Object> params) {
        log.info("Mock创建视频任务: prompt={}, images={}, params={}", prompt, images, params);
        // 返回模拟的任务ID
        return "mock-task-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public VideoTaskStatus queryTaskStatus(String taskId) {
        log.debug("Mock查询视频任务: taskId={}", taskId);
        
        // 模拟一个进行中的任务
        VideoTaskStatus status = new VideoTaskStatus();
        status.setStatus("processing");
        status.setProgress(75);
        
        return status;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
