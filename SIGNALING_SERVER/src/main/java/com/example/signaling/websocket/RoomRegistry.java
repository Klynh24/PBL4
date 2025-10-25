package com.example.signaling.websocket;

import com.example.signaling.security.ClientPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class RoomRegistry {

    private static final Logger log = LoggerFactory.getLogger(RoomRegistry.class);

    private final Map<String, Set<SessionDescriptor>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    public void joinRoom(String roomId, WebSocketSession session, ClientPrincipal principal) {
        SessionDescriptor descriptor = new SessionDescriptor(session, principal);
        rooms.compute(roomId, (key, value) -> {
            Set<SessionDescriptor> sessions = value == null ? new CopyOnWriteArraySet<>() : value;
            sessions.add(descriptor);
            return sessions;
        });
        sessionToRoom.put(session.getId(), roomId);
    }

    public Optional<String> findRoomForSession(WebSocketSession session) {
        return Optional.ofNullable(sessionToRoom.get(session.getId()));
    }

    public void leaveRoom(WebSocketSession session) {
        String sessionId = session.getId();
        String roomId = sessionToRoom.remove(sessionId);
        if (roomId == null) {
            return;
        }
        rooms.computeIfPresent(roomId, (key, value) -> {
            value.removeIf(descriptor -> descriptor.sessionId.equals(sessionId));
            if (value.isEmpty()) {
                return null;
            }
            return value;
        });
    }

    public void removeSession(WebSocketSession session) {
        leaveRoom(session);
    }

    public void broadcastLocal(String roomId, String payload, String excludeSessionId, String targetSubject) {
        Set<SessionDescriptor> sessions = rooms.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (SessionDescriptor descriptor : sessions) {
            if (descriptor.sessionId.equals(excludeSessionId)) {
                continue;
            }
            if (targetSubject != null && !targetSubject.equals(descriptor.principal.subject())) {
                continue;
            }
            WebSocketSession session = descriptor.session;
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("Failed to send message to session {} in room {}: {}", session.getId(), roomId,
                        e.getMessage());
                try {
                    session.close();
                } catch (IOException closeEx) {
                    log.debug("Failed to close session {}", session.getId(), closeEx);
                }
                removeSession(session);
            }
        }
    }

    private record SessionDescriptor(WebSocketSession session, ClientPrincipal principal, String sessionId) {
        private SessionDescriptor(WebSocketSession session, ClientPrincipal principal) {
            this(session, principal, session.getId());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof SessionDescriptor that))
                return false;
            return sessionId.equals(that.sessionId);
        }

        @Override
        public int hashCode() {
            return sessionId.hashCode();
        }
    }
}
