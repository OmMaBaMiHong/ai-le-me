package org.aileme.shejiao.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.aileme.shejiao.domain.entity.app.UserVideoEntity;
import org.aileme.shejiao.domain.param.app.GenerateVideoForm;
import org.aileme.shejiao.domain.param.app.PublishVideoForm;
import org.aileme.shejiao.domain.vo.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户AI视频服务接口
 *
 * @author system
 * @date 2026-02-13
 */
public interface UserVideoService extends IService<UserVideoEntity> {

    /**
     * 获取可用的视频模板列表
     *
     * @param category 场景分类（可为null表示全部）
     * @return 模板列表
     */
    List<VideoTemplateVo> getAvailableTemplates(String category);

    /**
     * 获取所有可用的模板场景分类列表
     *
     * @return 分类列表 [{category, categoryName}]
     */
    List<Map<String, String>> getTemplateCategories();

    /**
     * 生成自我介绍视频
     *
     * @param uid   用户ID
     * @param form  生成请求
     * @return 生成结果
     */
    VideoGenerateVo generateVideo(Integer uid, GenerateVideoForm form);

    /**
     * 查询视频生成状态
     *
     * @param videoId 视频ID
     * @param uid     用户ID
     * @return 状态信息
     */
    VideoStatusVo getVideoStatus(Integer videoId, Integer uid);

    /**
     * 发布视频到动态
     *
     * @param videoId 视频ID
     * @param uid     用户ID
     * @param form    发布请求
     * @return 帖子ID
     */
    Integer publishVideo(Integer videoId, Integer uid, PublishVideoForm form);

    /**
     * 获取用户视频列表
     *
     * @param uid   用户ID
     * @param page  页码
     * @param limit 每页数量
     * @return 视频列表
     */
    List<UserVideoVo> getUserVideoList(Integer uid, Integer page, Integer limit);

    /**
     * 获取可播放的视频实体列表（会尽量刷新临时失效的视频地址）
     *
     * @param videoIds 视频ID集合
     * @return 视频实体列表
     */
    List<UserVideoEntity> getPlayableVideos(Collection<Integer> videoIds);

    /**
     * 获取可播放的视频实体（会尽量刷新临时失效的视频地址）
     *
     * @param videoId 视频ID
     * @return 视频实体
     */
    UserVideoEntity getPlayableVideoById(Integer videoId);

    /**
     * 获取视频详情
     *
     * @param videoId 视频ID
     * @param uid     用户ID
     * @return 视频详情
     */
    UserVideoVo getVideoDetail(Integer videoId, Integer uid);

    /**
     * 删除视频
     *
     * @param videoId 视频ID
     * @param uid     用户ID
     */
    void deleteVideo(Integer videoId, Integer uid);

    /**
     * 处理AI生成回调
     *
     * @param videoId 视频ID
     * @param videoUrl 视频URL
     * @param coverUrl 封面URL
     * @param duration 时长
     */
    void handleGenerateCallback(Integer videoId, String videoUrl, String coverUrl, Integer duration);

    /**
     * 处理生成失败
     *
     * @param videoId  视频ID
     * @param errorMsg 错误信息
     */
    void handleGenerateError(Integer videoId, String errorMsg);

    /**
     * 更新生成进度
     *
     * @param videoId  视频ID
     * @param progress 进度百分比
     */
    void updateProgress(Integer videoId, Integer progress);
}
