package com.example.signaling.websocket;

import com.example.signaling.config.SignalingProperties;
import com.example.signaling.messaging.RoomMessagePublisher;
import com.example.signaling.model.SignalingMessage;
import com.example.signaling.security.ClientPrincipal;
import com.example.signaling.support.InstanceIdProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketHandler.class);
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "join", "leave", "offer", "answer", "candidate", "ping");

    private final ObjectMapper objectMapper;
    private final RoomRegistry roomRegistry;
    private final SignalingProperties properties;
    private final RoomMessagePublisher roomMessagePublisher;
    private final InstanceIdProvider instanceIdProvider;

    public SignalingWebSocketHandler(ObjectMapper objectMapper,
            RoomRegistry roomRegistry,
            SignalingProperties properties,
            RoomMessagePublisher roomMessagePublisher,
            InstanceIdProvider instanceIdProvider) {
        this.objectMapper = objectMapper;
        this.roomRegistry = roomRegistry;
        this.properties = properties;
        this.roomMessagePublisher = roomMessagePublisher;
        this.instanceIdProvider = instanceIdProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("Session {} connected", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (message.getPayloadLength() > properties.getMaxPayloadBytes()) {
            log.warn("Dropping oversized message from session {} ({} bytes)", session.getId(),
                    message.getPayloadLength());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // === LẤY PRINCIPAL AN TOÀN ===
        ClientPrincipal principal = properties.getAuth().isEnabled()
                ? (ClientPrincipal) session.getAttributes().get(ClientPrincipal.ATTRIBUTE_KEY)
                : new ClientPrincipal("anonymous", Map.of());

        // Nếu auth bật nhưng không có principal → từ chối
        if (properties.getAuth().isEnabled() && principal == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing authentication context"));
            return;
        }

        // === Parse message ===
        SignalingMessage signalingMessage;
        try {
            signalingMessage = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        } catch (JsonProcessingException e) {
            log.debug("Invalid payload from session {}: {}", session.getId(), e.getOriginalMessage());
            session.close(CloseStatus.BAD_DATA.withReason("Invalid signaling payload"));
            return;
        }

        if (signalingMessage.getType() == null || !SUPPORTED_TYPES.contains(signalingMessage.getType().toLowerCase())) {
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Unsupported signaling type\"}"));
            return;
        }

        // === XỬ LÝ THEO LOẠI ===
        switch (signalingMessage.getType().toLowerCase()) {
            case "offer" -> handleRawRelay(session, principal, signalingMessage, message.getPayload());
            case "answer" -> handleRawRelay(session, principal, signalingMessage, message.getPayload());
            case "candidate" -> handleRawRelay(session, principal, signalingMessage, message.getPayload());
            case "join" -> handleJoin(session, principal, signalingMessage);
            case "leave" -> handleLeave(session, principal, signalingMessage);
            case "ping" -> handlePing(session);
            default -> handleRelay(session, principal, signalingMessage);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        roomRegistry.findRoomForSession(session).ifPresent(roomId -> {
            roomRegistry.removeSession(session);
            broadcastLifecycleEvent(roomId, "peer-left", session, Map.of(
                    "reason", status.getReason() != null ? status.getReason() : status.toString()));
        });
        log.debug("Session {} closed: {}", session.getId(), status);
    }

    private void handleRawRelay(WebSocketSession session, ClientPrincipal principal, SignalingMessage message,
            String rawPayload) {
        roomRegistry.findRoomForSession(session).ifPresentOrElse(roomId -> {
            if (message.requiresRoom() && (message.getRoomId() == null ||
                    !roomId.equals(message.getRoomId()))) {
                sendErrorAsync(session, "Invalid room for message");
                return;
            }
            String target = message.getTarget();
            if (target != null && target.isBlank()) {
                target = null;
            }
            roomRegistry.broadcastLocal(roomId, rawPayload, session.getId(), target);
            roomMessagePublisher.publish(roomId, rawPayload, session.getId(), instanceIdProvider.getInstanceId(),
                    target);
        }, () -> sendErrorAsync(session, "Join a room before exchanging signaling data"));
    }

    private void handleJoin(WebSocketSession session, ClientPrincipal principal, SignalingMessage message)
            throws IOException {
        String roomId = message.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"roomId is required\"}"));
            return;
        }

        String subject = principal.subject(); // ← an toàn: AnonymousPrincipal trả về "anonymous"

        roomRegistry.joinRoom(roomId, session, principal);
        ObjectNode ack = objectMapper.createObjectNode();
        ack.put("type", "joined");
        ack.put("roomId", roomId);
        ack.put("subject", subject);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        broadcastLifecycleEvent(roomId, "peer-joined", session, Map.of("subject", subject));
        log.info("Subject {} joined room {} (session {})", subject, roomId, session.getId());
    }

    private void handleLeave(WebSocketSession session, ClientPrincipal principal, SignalingMessage message) {
        roomRegistry.findRoomForSession(session).ifPresent(roomId -> {
            roomRegistry.leaveRoom(session);
            broadcastLifecycleEvent(roomId, "peer-left", session, Map.of("subject", principal.subject()));
        });
    }

    private void handlePing(WebSocketSession session) throws IOException {
        session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
    }

    private void handleRelay(WebSocketSession session, ClientPrincipal principal, SignalingMessage message) {
        roomRegistry.findRoomForSession(session).ifPresentOrElse(roomId -> {
            if (message.requiresRoom() && (message.getRoomId() == null || !roomId.equals(message.getRoomId()))) {
                sendErrorAsync(session, "Invalid room for message");
                return;
            }
            ObjectNode outbound = objectMapper.createObjectNode();
            outbound.put("type", message.getType());
            outbound.put("roomId", roomId);
            outbound.put("sender", principal.subject());
            if (message.getTarget() != null) {
                outbound.put("target", message.getTarget());
            }
            outbound.set("payload", objectMapper.valueToTree(message.getPayload()));
            String serialized;
            try {
                serialized = objectMapper.writeValueAsString(outbound);
            } catch (JsonProcessingException e) {
                sendErrorAsync(session, "Failed to serialize message");
                return;
            }
            String targetSubject = message.getTarget();
            roomRegistry.broadcastLocal(roomId, serialized, session.getId(), targetSubject);
            roomMessagePublisher.publish(roomId, serialized, session.getId(), instanceIdProvider.getInstanceId(),
                    targetSubject);
        }, () -> sendErrorAsync(session, "Join a room before exchanging signaling data"));
    }

    private void broadcastLifecycleEvent(String roomId, String eventType, WebSocketSession origin,
            Map<String, Object> data) {
        ObjectNode outbound = objectMapper.createObjectNode();
        outbound.put("type", eventType);
        outbound.put("roomId", roomId);
        if (data != null)
            data.forEach((key, value) -> outbound.set(key, objectMapper.valueToTree(value)));
        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(outbound);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize lifecycle event {}: {}", eventType, e.getMessage());
            return;
        }
        roomRegistry.broadcastLocal(roomId, serialized, origin.getId(), null);
        roomMessagePublisher.publish(roomId, serialized, origin.getId(), instanceIdProvider.getInstanceId(), null);
    }

    private void sendErrorAsync(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "message", message))));
        } catch (IOException e) {
            log.debug("Failed to send error message to {}: {}", session.getId(), e.getMessage());
        }
    }
}