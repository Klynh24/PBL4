package pbl.backend.kchi.websocket;

import java.util.ArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import pbl.backend.kchi.realtime.RealtimeRoomService;
import pbl.backend.kchi.realtime.RealtimeRoomService.LeaveResult;
import pbl.backend.kchi.websocket.dto.RoomEventPayload;

@Component
public class RoomWebSocketSessionEventListener {

    private final RealtimeRoomService realtimeRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomWebSocketSessionEventListener(RealtimeRoomService realtimeRoomService,
            SimpMessagingTemplate messagingTemplate) {
        this.realtimeRoomService = realtimeRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        LeaveResult res = realtimeRoomService.leaveBySession(sessionId);
        if (res == null)
            return;

        Long roomId = res.roomId();
        if (roomId == null)
            return;

        String topic = "/topic/room/" + roomId;
        messagingTemplate.convertAndSend(topic, RoomEventPayload.leave(roomId, res.userId()));
        messagingTemplate.convertAndSend(topic, RoomEventPayload.userList(roomId,
                new ArrayList<>(realtimeRoomService.getActiveUsers(roomId))));
    }
}
