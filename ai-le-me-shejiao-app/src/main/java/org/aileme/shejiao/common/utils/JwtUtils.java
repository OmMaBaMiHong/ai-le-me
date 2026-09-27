package org.aileme.shejiao.common.utils;

import cn.dev33.satoken.stp.StpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.common.exception.LinfengException;

/**
 * JWT工具类：基于 Sa-Token 实现的 JWT 令牌管理
 * 兼容旧代码的接口，底层使用 Sa-Token
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "oauth2.jwt")
@Data
public class JwtUtils {

    private String header = "token";
    
    private long accessTokenValiditySeconds = 604800;

    /**
     * 生成JWT令牌 (使用 Sa-Token)
     */
    public String generateToken(Object username) {
        return generateTokenWithTenant(username, null);
    }
    
    /**
     * 生成带租户ID的JWT令牌 (使用 Sa-Token)
     * 注意：实际token生成由Sa-Token管理，此方法仅返回当前token值
     */
    public String generateTokenWithTenant(Object username, Long tenantId) {
        try {
            // 使用 Sa-Token 登录并生成 token
            // 需要先调用 StpUtil.login() 来生成 token
            log.warn("generateTokenWithTenant 方法已过时，请使用 LoginHelper.login() 方法登录并获取token");
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            log.error("获取JWT令牌失败", e);
            throw new RuntimeException("获取JWT令牌失败");
        }
    }

    /**
     * 从JWT令牌中提取用户名 (使用 Sa-Token)
     */
    public String extUserId(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            // 使用 Sa-Token 获取 loginId (用户ID/用户名)
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null ? String.valueOf(loginId) : null;
        } catch (Exception e) {
            log.error("解析JWT令牌用户名失败", e);
            return null;
        }
    }

    /**
     * 验证JWT令牌有效性 (使用 Sa-Token)
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            // 使用 Sa-Token 验证 token 是否有效
            Object loginId = StpUtil.getLoginIdByToken(token);
            return loginId != null;
        } catch (Exception e) {
            log.error("验证JWT令牌失败", e);
            return false;
        }
    }
    
    /**
     * 验证JWT令牌有效性并提取用户ID (使用 Sa-Token)
     */
    public String validateTokenAndExtUserId(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new LinfengException("token无效，请重新登录", HttpStatus.UNAUTHORIZED.value());
            }
            
            // 使用 Sa-Token 验证并获取 loginId
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                throw new LinfengException("token无效，请重新登录", HttpStatus.UNAUTHORIZED.value());
            }
            return String.valueOf(loginId);
        } catch (LinfengException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证JWT令牌失败", e);
            throw new LinfengException("token无效，请重新登录", HttpStatus.UNAUTHORIZED.value());
        }
    }
    
    public String getHeader() {
        return header;
    }
    
    /**
     * 从JWT令牌中提取用户ID (使用 Sa-Token)
     */
    public Integer getUserIdFromToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                return Integer.parseInt(String.valueOf(loginId));
            }
        } catch (Exception e) {
            log.error("解析JWT令牌用户ID失败", e);
        }
        return null;
    }
    
    /**
     * 从JWT令牌中提取租户ID (使用 Sa-Token)
     */
    public Long getTenantIdFromToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }
            
            // 使用 Sa-Token 的 extra 功能获取租户ID
            Object tenantId = StpUtil.getExtra(token, "tenantId");
            if (tenantId != null) {
                return Long.parseLong(String.valueOf(tenantId));
            }
        } catch (Exception e) {
            log.error("解析JWT令牌租户ID失败", e);
        }
        return null;
    }
}