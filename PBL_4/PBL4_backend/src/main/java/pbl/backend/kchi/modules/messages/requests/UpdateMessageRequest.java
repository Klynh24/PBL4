package pbl.backend.kchi.modules.messages.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMessageRequest {
    private String text;

    @NotNull(message = "Cuộc hội thoại không được bỏ trống")
    private Long conversationId;

    private Long userId;

}