package com.example.signaling.config;

import com.example.signaling.security.JwtService;
import com.example.signaling.websocket.JwtHandshakeInterceptor;
import com.example.signaling.websocket.SignalingWebSocketHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingWebSocketHandler webSocketHandler;
    private final SignalingProperties properties;
    private final ObjectProvider<JwtService> jwtServiceProvider;

    public WebSocketConfig(SignalingWebSocketHandler webSocketHandler,
            SignalingProperties properties,
            ObjectProvider<JwtService> jwtServiceProvider) {
        this.webSocketHandler = webSocketHandler;
        this.properties = properties;
        this.jwtServiceProvider = jwtServiceProvider;
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(properties.getMaxPayloadBytes());
        container.setMaxBinaryMessageBufferSize(properties.getMaxPayloadBytes());
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var websocketProps = properties.getWebsocket();
        var registration = registry
                .addHandler(webSocketHandler, websocketProps.getPath())
                .addInterceptors(new JwtHandshakeInterceptor(properties, jwtServiceProvider.getIfAvailable()))
                .setAllowedOrigins(websocketProps.getAllowedOrigins());
        if (websocketProps.isAllowSockJs()) {
            registration.withSockJS();
        }
    }
}
