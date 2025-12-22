package pbl.backend.kchi.websocket;

import java.security.Principal;
import java.util.ArrayList;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import pbl.backend.kchi.realtime.RealtimeRoomService;
import pbl.backend.kchi.websocket.dto.RoomChatMessageRequest;
import pbl.backend.kchi.websocket.dto.RoomEventPayload;
import pbl.backend.kchi.websocket.dto.RoomJoinLeaveRequest;

@Controller
public class RoomWebSocketController {

    private final RealtimeRoomService realtimeRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomWebSocketController(RealtimeRoomService realtimeRoomService, SimpMessagingTemplate messagingTemplate) {
        this.realtimeRoomService = realtimeRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room.join")
    public void joinRoom(@Valid @Payload RoomJoinLeaveRequest request, Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        String userId = principal != null ? principal.getName() : null;
        if (userId == null) {
            throw new IllegalStateException("Unauthenticated WebSocket session");
        }

        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : null;
        if (sessionId == null) {
            throw new IllegalStateException("Missing STOMP sessionId");
        }

        Long roomId = request.getRoomId();
        realtimeRoomService.joinRoom(roomId, userId, sessionId);

        String topic = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(topic, RoomEventPayload.join(roomId, userId));
        messagingTemplate.convertAndSend(topic, RoomEventPayload.userList(roomId,
                new ArrayList<>(realtimeRoomService.getActiveUsers(roomId))));
    }

    @MessageMapping("/room.leave")
    public void leaveRoom(@Valid @Payload RoomJoinLeaveRequest request, Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        String userId = principal != null ? principal.getName() : null;
        if (userId == null) {
            throw new IllegalStateException("Unauthenticated WebSocket session");
        }

        String sessionId = headerAccessor != null ? headerAccessor.getSessionId() : null;
        if (sessionId == null) {
            throw new IllegalStateException("Missing STOMP sessionId");
        }

        Long roomId = request.getRoomId();
        // Remove the session mapping; the roomId from request is used for broadcast
        // topic.
        realtimeRoomService.leaveBySession(sessionId);

        String topic = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(topic, RoomEventPayload.leave(roomId, userId));
        messagingTemplate.convertAndSend(topic, RoomEventPayload.userList(roomId,
                new ArrayList<>(realtimeRoomService.getActiveUsers(roomId))));
    }

    @MessageMapping("/room.chat")
    public void sendRoomChat(@Valid @Payload RoomChatMessageRequest request, Principal principal) {
        String userId = principal != null ? principal.getName() : null;
        if (userId == null) {
            throw new IllegalStateException("Unauthenticated WebSocket session");
        }

        Long roomId = request.getRoomId();
        String topic = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(topic, RoomEventPayload.chat(roomId, userId, request.getText()));
    }
}
