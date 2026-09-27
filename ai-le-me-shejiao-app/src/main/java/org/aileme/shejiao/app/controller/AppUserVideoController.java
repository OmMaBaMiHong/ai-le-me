package org.aileme.shejiao.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aileme.system.service.ISysThirdPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.aileme.shejiao.api.service.UserVideoService;
import org.aileme.shejiao.app.annotation.Login;
import org.aileme.shejiao.app.annotation.LoginUser;
import org.aileme.shejiao.app.runtime.compliance.MiniAppFilingFeatureService;
import org.aileme.shejiao.common.utils.Result;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.domain.param.app.GenerateVideoForm;
import org.aileme.shejiao.domain.param.app.PublishVideoForm;
import org.aileme.shejiao.domain.vo.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 移动端 - AI视频生成
 *
 * @author system
 * @date 2026-02-13
 */
@Slf4j
@Tag(name = "移动端——AI视频生成")
@RestController
@RequestMapping("/app/video")
public class AppUserVideoController {

    @Autowired
    private UserVideoService userVideoService;

    @Autowired
    private ISysThirdPartyService thirdPartyService;

    @Autowired
    private MiniAppFilingFeatureService miniAppFilingFeatureService;

    /**
     * 获取视频模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "获取视频模板列表")
    public Result<List<VideoTemplateVo>> getTemplates(
            @RequestParam(required = false) String category) {
        miniAppFilingFeatureService.requireAiVideoEnabled();
        List<VideoTemplateVo> templates = userVideoService.getAvailableTemplates(category);
        return new Result<List<VideoTemplateVo>>().ok(templates);
    }

    /**
     * 获取模板场景分类列表
     */
    @GetMapping("/categories")
    @Operation(summary = "获取模板场景分类列表")
    public Result<List<Map<String, String>>> getCategories() {
        miniAppFilingFeatureService.requireAiVideoEnabled();
        List<Map<String, String>> categories = userVideoService.getTemplateCategories();
        return new Result<List<Map<String, String>>>().ok(categories);
    }

    /**
     * 生成视频
     */
    @Login
    @PostMapping("/generate")
    @Operation(summary = "生成自我介绍视频")
    public Result<VideoGenerateVo> generate(
            @Valid @RequestBody GenerateVideoForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        miniAppFilingFeatureService.requireAiVideoEnabled();
        VideoGenerateVo result = userVideoService.generateVideo(user.getUid(), form);
        return new Result<VideoGenerateVo>().ok(result);
    }

    /**
     * 查询生成状态
     */
    @Login
    @GetMapping("/status/{id}")
    @Operation(summary = "查询视频生成状态")
    public Result<VideoStatusVo> getStatus(
            @PathVariable("id") Integer videoId,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        VideoStatusVo status = userVideoService.getVideoStatus(videoId, user.getUid());
        return new Result<VideoStatusVo>().ok(status);
    }

    /**
     * 发布视频到动态
     */
    @Login
    @PostMapping("/publish/{id}")
    @Operation(summary = "发布视频到动态")
    public Result<PublishResultVo> publish(
            @PathVariable("id") Integer videoId,
            @RequestBody PublishVideoForm form,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        Integer postId = userVideoService.publishVideo(videoId, user.getUid(), form);
        return new Result<PublishResultVo>().ok(new PublishResultVo(postId));
    }

    /**
     * 我的视频列表
     */
    @Login
    @GetMapping("/list")
    @Operation(summary = "我的视频列表")
    @Parameters({
            @Parameter(name = "page", description = "分页页码", required = true),
            @Parameter(name = "limit", description = "每页数量")
    })
    public Result<List<UserVideoVo>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        List<UserVideoVo> videos = userVideoService.getUserVideoList(user.getUid(), page, limit);
        return new Result<List<UserVideoVo>>().ok(videos);
    }

    /**
     * 视频详情
     */
    @Login
    @GetMapping("/detail/{id}")
    @Operation(summary = "视频详情")
    public Result<UserVideoVo> detail(
            @PathVariable("id") Integer videoId,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        UserVideoVo video = userVideoService.getVideoDetail(videoId, user.getUid());
        return new Result<UserVideoVo>().ok(video);
    }

    /**
     * 删除视频
     */
    @Login
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除视频")
    public Result<Void> delete(
            @PathVariable("id") Integer videoId,
            @Parameter(hidden = true) @LoginUser AppUserEntity user) {
        userVideoService.deleteVideo(videoId, user.getUid());
        return new Result<Void>().ok();
    }

    /**
     * AI生成回调接口（内部调用）
     */
    @PostMapping("/callback/{id}")
    @Operation(summary = "AI生成回调（内部接口）")
    public Result<Void> callback(
            @PathVariable("id") Integer videoId,
            @RequestHeader(value = "X-Callback-Token", required = false) String callbackToken,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam String videoUrl,
            @RequestParam(required = false) String coverUrl,
            @RequestParam(required = false) Integer duration) {
        if (!verifyCallbackToken(callbackToken, token)) {
            return new Result<Void>().error("回调鉴权失败");
        }
        userVideoService.handleGenerateCallback(videoId, videoUrl, coverUrl, duration);
        return new Result<Void>().ok();
    }

    /**
     * AI生成错误回调（内部调用）
     */
    @PostMapping("/error/{id}")
    @Operation(summary = "AI生成错误回调（内部接口）")
    public Result<Void> error(
            @PathVariable("id") Integer videoId,
            @RequestHeader(value = "X-Callback-Token", required = false) String callbackToken,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam String errorMsg) {
        if (!verifyCallbackToken(callbackToken, token)) {
            return new Result<Void>().error("回调鉴权失败");
        }
        userVideoService.handleGenerateError(videoId, errorMsg);
        return new Result<Void>().ok();
    }

    /**
     * 回调鉴权：当配置了 callback_token 时必须校验
     */
    private boolean verifyCallbackToken(String headerToken, String queryToken) {
        String expected;
        try {
            expected = thirdPartyService.getConfigValue("video", "callback_token");
        } catch (Exception e) {
            log.warn("读取视频回调token失败，忽略鉴权: {}", e.getMessage());
            return true;
        }

        if (StringUtils.isBlank(expected)) {
            return true;
        }
        String actual = StringUtils.firstNonBlank(headerToken, queryToken);
        boolean passed = StringUtils.equals(expected, actual);
        if (!passed) {
            log.warn("视频回调鉴权失败");
        }
        return passed;
    }
}
