package org.aileme.shejiao.app.service.ai.video;

import java.util.List;
import java.util.Map;

/**
 * AI视频生成服务接口
 * 
 * @author system
 * @date 2026-03-01
 */
public interface AIVideoProvider {

    /**
     * 创建视频生成任务
     *
     * @param prompt 提示词
     * @param images 素材图片URL列表
     * @param params 额外参数(时长、宽高比等)
     * @return 任务ID
     */
    String createVideoTask(String prompt, List<String> images, Map<String, Object> params);

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    VideoTaskStatus queryTaskStatus(String taskId);

    /**
     * 获取提供商名称
     *
     * @return 提供商标识(jimeng/kling/doubao等)
     */
    String getProviderName();

    /**
     * 视频任务状态
     */
    class VideoTaskStatus {
        /** 状态: pending-排队中, processing-生成中, succeeded-成功, failed-失败 */
        private String status;
        /** 进度百分比 0-100 */
        private Integer progress;
        /** 视频URL(生成成功后) */
        private String videoUrl;
        /** 封面URL */
        private String coverUrl;
        /** 视频时长(秒) */
        private Integer duration;
        /** 错误信息(失败时) */
        private String errorMessage;

        public VideoTaskStatus() {}

        public VideoTaskStatus(String status, Integer progress) {
            this.status = status;
            this.progress = progress;
        }

        // Getters and Setters
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getProgress() {
            return progress;
        }

        public void setProgress(Integer progress) {
            this.progress = progress;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getCoverUrl() {
            return coverUrl;
        }

        public void setCoverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
