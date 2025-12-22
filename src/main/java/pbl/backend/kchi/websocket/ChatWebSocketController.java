package pbl.backend.kchi.websocket;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import pbl.backend.kchi.websocket.dto.ChatMessagePayload;
import pbl.backend.kchi.websocket.dto.PrivateChatMessageRequest;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Valid @Payload PrivateChatMessageRequest request, Principal principal) {
        String senderId = principal != null ? principal.getName() : null;
        if (senderId == null) {
            throw new IllegalStateException("Unauthenticated WebSocket session");
        }

        ChatMessagePayload payload = new ChatMessagePayload(
                senderId,
                request.getRecipientId(),
                request.getText(),
                System.currentTimeMillis());

        // Deliver to recipient
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getRecipientId()),
                "/queue/messages",
                payload);

        // Echo back to sender so UI can update immediately
        messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/messages",
                payload);
    }
}
