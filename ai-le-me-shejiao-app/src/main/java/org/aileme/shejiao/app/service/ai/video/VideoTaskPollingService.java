package org.aileme.shejiao.app.service.ai.video;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.UserVideoService;
import org.aileme.shejiao.domain.entity.app.UserVideoEntity;

import java.util.List;

/**
 * AI 视频任务轮询核心服务。
 * 业务逻辑统一沉到 shejiao-app，便于 Spring 单体直启，也便于外部调度框架复用。
 */
@Slf4j
@Service
public class VideoTaskPollingService {

    @Autowired
    private UserVideoService userVideoService;

    @Autowired
    private AIVideoStrategyManager videoStrategyManager;

    @Value("${shejiao.ai-video.polling.timeout-minutes:15}")
    private int timeoutMinutes;

    public String doPollPendingTasks() {
        List<UserVideoEntity> pendingTasks = userVideoService.lambdaQuery()
                .eq(UserVideoEntity::getStatus, UserVideoEntity.STATUS_GENERATING)
                .isNotNull(UserVideoEntity::getTaskId)
                .ne(UserVideoEntity::getTaskId, "")
                .list();

        if (pendingTasks.isEmpty()) {
            return "无待处理任务";
        }

        log.info("[视频轮询] 发现{}个待处理的视频任务", pendingTasks.size());

        int finishedCount = 0;
        int failCount = 0;

        for (UserVideoEntity video : pendingTasks) {
            try {
                boolean finished = pollSingleTask(video);
                if (finished) {
                    finishedCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.error("[视频轮询] 轮询失败: videoId={}, taskId={}, error={}",
                        video.getId(), video.getTaskId(), e.getMessage(), e);
            }
        }

        String result = String.format("处理%d个任务: %d个已完结, %d个异常, %d个进行中",
                pendingTasks.size(),
                finishedCount,
                failCount,
                pendingTasks.size() - finishedCount - failCount);

        log.info("[视频轮询] {}", result);
        return result;
    }

    private boolean pollSingleTask(UserVideoEntity video) {
        String taskId = video.getTaskId();
        Integer videoId = video.getId();

        if (isTaskTimeout(video)) {
            log.warn("[视频轮询] 任务超时: videoId={}, taskId={}", videoId, taskId);
            userVideoService.handleGenerateError(videoId, "视频生成超时，请稍后重试");
            return true;
        }

        String providerCode = StringUtils.defaultIfBlank(video.getAiModel(), videoStrategyManager.getCurrentProviderCode());
        AIVideoProvider.VideoTaskStatus taskStatus = videoStrategyManager.queryTaskStatus(taskId, providerCode);

        log.info("[视频轮询] videoId={}, taskId={}, provider={}, status={}, progress={}",
                videoId, taskId, providerCode, taskStatus.getStatus(), taskStatus.getProgress());

        if (taskStatus.getProgress() != null) {
            userVideoService.updateProgress(videoId, taskStatus.getProgress());
        }

        switch (taskStatus.getStatus()) {
            case "succeeded":
                log.info("[视频轮询] 生成成功: videoId={}, videoUrl={}", videoId, taskStatus.getVideoUrl());
                userVideoService.handleGenerateCallback(
                        videoId,
                        taskStatus.getVideoUrl(),
                        taskStatus.getCoverUrl(),
                        taskStatus.getDuration()
                );
                return true;
            case "failed":
                String errorMsg = StringUtils.defaultIfBlank(taskStatus.getErrorMessage(), "视频生成失败");
                log.warn("[视频轮询] 生成失败: videoId={}, error={}", videoId, errorMsg);
                userVideoService.handleGenerateError(videoId, errorMsg);
                return true;
            case "pending":
            case "processing":
                return false;
            default:
                log.warn("[视频轮询] 未知状态: videoId={}, status={}", videoId, taskStatus.getStatus());
                return false;
        }
    }

    private boolean isTaskTimeout(UserVideoEntity video) {
        if (video.getCreateTime() == null) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - video.getCreateTime().getTime();
        return elapsed > timeoutMinutes * 60L * 1000L;
    }
}
