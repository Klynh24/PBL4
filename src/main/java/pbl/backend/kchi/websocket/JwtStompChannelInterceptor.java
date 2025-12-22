package pbl.backend.kchi.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import pbl.backend.kchi.modules.users.services.impl.CustomUserDetailService;
import pbl.backend.kchi.services.JwtService;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailService customUserDetailService;

    public JwtStompChannelInterceptor(JwtService jwtService, CustomUserDetailService customUserDetailService) {
        this.jwtService = jwtService;
        this.customUserDetailService = customUserDetailService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = firstHeader(accessor, "Authorization");
            if (authHeader == null) {
                authHeader = firstHeader(accessor, "authorization");
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or invalid Authorization header for WebSocket CONNECT");
            }

            String jwt = authHeader.substring(7);

            String userId = jwtService.getUserIdFromJwt(jwt);
            if (userId == null) {
                throw new IllegalArgumentException("Invalid JWT: missing subject");
            }

            if (!jwtService.isIssuerToken(jwt)) {
                throw new IllegalArgumentException("Invalid JWT issuer");
            }

            if (jwtService.isBlacklistedToken(jwt)) {
                throw new IllegalArgumentException("Token is blacklisted");
            }

            UserDetails userDetails = customUserDetailService.loadUserByUsername(userId);

            // Use userId as principal name so convertAndSendToUser(userId, ...) works.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    userDetails.getAuthorities());

            accessor.setUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        return message;
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        return accessor.getFirstNativeHeader(name);
    }
}
