package org.aileme.shejiao.app.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.aileme.shejiao.common.utils.JwtUtils;

import java.util.Map;

@Slf4j
@Component
public class HandShakeInterceptor  extends HttpSessionHandshakeInterceptor {

    @Autowired
    private JwtUtils jwtUtils;
    /*
     * 在WebSocket连接建立之前的操作，以鉴权为例
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
        String token = serverRequest.getServletRequest().getParameter("token");
        if (StringUtils.isNotBlank(token) && jwtUtils.validateToken(token)) {
           //此处将token传递到WebSocketController，必须写否则拿不到数据
            attributes.put("token", token);
            return super.beforeHandshake(request, response, wsHandler, attributes);
        }
        return true;
    }



    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {
        // 省略根据业务处理
    }
}
