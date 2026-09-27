package org.aileme.shejiao.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.aileme.shejiao.common.utils.JwtUtils;
import org.aileme.shejiao.common.utils.TenantContextHolder;

import java.io.IOException;

/**
 * 租户过滤器
 * 在请求处理前解析JWT中的租户ID并设置到上下文中
 *
 * 注意：App端（社交应用）不需要多租户功能，此过滤器仅处理管理端请求
 */
@Slf4j
@Component
@Order(1) // 设置优先级，确保在认证过滤器之后执行
public class TenantFilter implements Filter {

    private final JwtUtils jwtUtils;

    public TenantFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 检查是否是App端请求（/app/**路径）
            String requestURI = httpRequest.getRequestURI();
            if (requestURI != null && requestURI.startsWith("/app/")) {
                // App端不需要租户功能，直接跳过
                log.debug("App端请求，跳过租户过滤器: {}", requestURI);
                chain.doFilter(request, response);
                return;
            }

            // 从请求头中获取token
            String token = extractToken(httpRequest);
            if (token != null) {
                // 从JWT中解析租户ID
                Long tenantId = jwtUtils.getTenantIdFromToken(token);
                if (tenantId != null) {
                    // 设置到上下文中
                    TenantContextHolder.setTenantId(tenantId);
                    log.debug("设置租户ID到上下文: {}", tenantId);
                }
            }

            // 继续执行过滤链
            chain.doFilter(request, response);
        } finally {
            // 请求完成后清理上下文
            TenantContextHolder.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        // 从Header中获取token
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 从自定义Header中获取token
        String token = request.getHeader("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        // 从参数中获取token
        token = request.getParameter("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }

        return null;
    }
}
