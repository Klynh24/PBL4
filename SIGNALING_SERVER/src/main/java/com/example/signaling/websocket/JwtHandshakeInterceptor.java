package com.example.signaling.websocket;

import com.example.signaling.config.SignalingProperties;
import com.example.signaling.security.ClientPrincipal;
import com.example.signaling.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final SignalingProperties properties;
    @Nullable
    private final JwtService jwtService;

    public JwtHandshakeInterceptor(SignalingProperties properties, @Nullable JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        boolean authEnabled = properties.getAuth().isEnabled();
        String token = extractBearerToken(request.getHeaders());
        if (token == null && request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }
        if (authEnabled) {
            if (jwtService == null) {
                return false;
            }
            Optional<ClientPrincipal> principal = jwtService.parse(token);
            if (principal.isEmpty()) {
                return false;
            }
            attributes.put(ClientPrincipal.ATTRIBUTE_KEY, principal.get());
        } else {
            ClientPrincipal principal = resolveBestEffortPrincipal(request, token);
            attributes.put(ClientPrincipal.ATTRIBUTE_KEY, principal);
        }
        return true;
    }

    @Override
    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                               org.springframework.http.server.ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private String extractBearerToken(HttpHeaders headers) {
        String header = headers.getFirst(HttpHeaders.AUTHORIZATION);
        return header != null && !header.isBlank() ? header : null;
    }

    private ClientPrincipal resolveBestEffortPrincipal(org.springframework.http.server.ServerHttpRequest request, String token) {
        if (jwtService != null) {
            Optional<ClientPrincipal> parsed = jwtService.parse(token);
            if (parsed.isPresent()) {
                return parsed.get();
            }
        }
        String fallbackId = "anonymous";
        if (request instanceof org.springframework.http.server.ServletServerHttpRequest servletRequest) {
            var servlet = servletRequest.getServletRequest();
            String clientId = servlet.getParameter("clientId");
            if (clientId == null) {
                clientId = servlet.getHeader("X-Client-Id");
            }
            if (clientId != null && !clientId.isBlank()) {
                fallbackId = clientId;
            }
        }
        return new ClientPrincipal(fallbackId, Map.of());
    }
}
