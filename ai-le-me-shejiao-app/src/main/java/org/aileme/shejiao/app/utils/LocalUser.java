/**
 * -----------------------------------
 * Copyright (c) 2021-2023
 * All rights reserved, Designed By my.hots.love
 * <p>
 * 商业版授权联系技术客服	 QQ:  3582996245
 * 严禁分享、盗用、转卖源码或非法牟利！
 * 版权所有 ，侵权必究！
 * -----------------------------------
 */
package org.aileme.shejiao.app.utils;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.aileme.shejiao.domain.entity.admin.AppUserEntity;
import org.aileme.shejiao.api.service.AppUserService;
import org.aileme.shejiao.common.utils.JwtUtils;

import java.util.Objects;

/**
 * 获取登录用户信息，如果未登录返回空
 * @author linfeng
 * @date 2022/4/7 11:49
 */
@Component("appLocalUser")
public class LocalUser {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AppUserService userService;


    public AppUserEntity getUser() {
        //获取request
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        //获取token
        String token = request.getHeader(jwtUtils.getHeader());
        if (StringUtils.isBlank(token)) {
            token = request.getParameter(jwtUtils.getHeader());
        }
        //凭证为空
        if (StringUtils.isBlank(token)) {
            return null;
        }
        boolean isValid = jwtUtils.validateToken(token);
        if (!isValid) {
            return null;
        }
        int userId = Integer.parseInt(jwtUtils.extUserId(token));
        AppUserEntity userInfo = org.aileme.common.redis.utils.RedisUtils.getCacheObject("userId:" + userId);
        if (userInfo != null) {
            return userInfo;
        }
        //重新获取用户信息
        AppUserEntity user = userService.getById(userId);
        org.aileme.common.redis.utils.RedisUtils.setCacheObject("userId:" + userId, user, Duration.ofSeconds(7200));
        return user;
    }
}
