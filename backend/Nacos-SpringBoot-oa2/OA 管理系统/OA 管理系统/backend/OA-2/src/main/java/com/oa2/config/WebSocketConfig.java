package com.oa2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 服务端推送前缀 /topic（广播）和 /queue（点对点）
        config.enableSimpleBroker("/topic", "/queue");
        // 客户端发消息前缀
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 端点，前端通过 SockJS 连接
        registry.addEndpoint("/ws/notification")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
