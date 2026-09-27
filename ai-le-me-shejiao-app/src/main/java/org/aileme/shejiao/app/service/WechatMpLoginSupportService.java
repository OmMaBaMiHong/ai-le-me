package org.aileme.shejiao.app.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.common.exception.LinfengException;
import org.aileme.shejiao.common.utils.JwtUtils;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;

import java.time.Duration;

import static org.aileme.shejiao.common.utils.RedisKeys.WX_LOGIN_CODE_;

/**
 * 微信公众号扫码登录辅助服务
 */
@Service
@Slf4j
public class WechatMpLoginSupportService {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private JwtUtils jwtUtils;

    public String syncUserAndCacheLoginToken(WxMpUser wxMpUser, String rawSceneId) {
        AppUserEntity appUser = appUserService.saveMpWxUser(wxMpUser);
        if (appUser == null || appUser.getUid() == null) {
            throw new LinfengException("公众号用户同步失败");
        }
        if (appUser.getStatus() != null && appUser.getStatus() == 1) {
            throw new LinfengException("该账号已被禁用");
        }

        String sceneId = normalizeSceneId(rawSceneId);
        if (StrUtil.isBlank(sceneId)) {
            log.info("公众号用户已同步，但当前不是登录扫码场景，uid={}", appUser.getUid());
            return null;
        }

        StpUtil.login(appUser.getUid(), new SaLoginParameter()
                .setTimeout(jwtUtils.getAccessTokenValiditySeconds())
                .setExtra("clientid", "app"));
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(token) && StpUtil.getTokenInfo() != null) {
            token = StpUtil.getTokenInfo().getTokenValue();
        }
        if (StrUtil.isBlank(token)) {
            throw new LinfengException("公众号扫码登录令牌生成失败");
        }

        RedisUtils.setCacheObject(WX_LOGIN_CODE_ + sceneId, token, Duration.ofMinutes(10));
        log.info("公众号扫码登录令牌已写入缓存，sceneId={}, uid={}", sceneId, appUser.getUid());
        return token;
    }

    public String normalizeSceneId(String rawSceneId) {
        String sceneId = StrUtil.trimToEmpty(rawSceneId);
        if (StrUtil.startWith(sceneId, "qrscene_")) {
            sceneId = StrUtil.removePrefix(sceneId, "qrscene_");
        }
        return StrUtil.trimToEmpty(sceneId);
    }
}
