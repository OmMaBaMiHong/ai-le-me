package org.aileme.shejiao.admin.utils;

import java.time.Duration;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aileme.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;

import java.util.Objects;

/**
 * 获取登录用户信息(Admin使用,基于Session)
 * @author linfeng
 * @date 2022/4/7 11:49
 */
@Component("localUser")
public class LocalUser {
    @Autowired
    private AppUserService userService;

    public AppUserEntity getUser() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        // Admin端从session获取userId
        Integer userId = (Integer) request.getSession().getAttribute("userId");
        if (userId == null) {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            } else {
                userId = Integer.parseInt(loginId.toString());
            }
        }
        AppUserEntity userInfo = RedisUtils.getCacheObject("userId:" + userId);
        if (userInfo != null) {
            return userInfo;
        }

        // 重新获取用户信息
        AppUserEntity user = userService.getById(userId);
        if (user != null) {
            RedisUtils.setCacheObject("userId:" + userId, user, Duration.ofSeconds(7200));
        }
        return user;
    }
}
