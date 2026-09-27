package org.aileme.shejiao.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 用于启用WebSocket功能，注册@ServerEndpoint注解的端点
 *
 * @author system
 * @date 2026-02-05
 */
@Configuration
public class WebSocketConfig {

    /**
     * 注册ServerEndpointExporter
     * 这个Bean会自动注册使用了@ServerEndpoint注解声明的WebSocket endpoint
     * 
     * 注意：如果使用外部Tomcat部署，不需要注入这个Bean，否则会报错
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
