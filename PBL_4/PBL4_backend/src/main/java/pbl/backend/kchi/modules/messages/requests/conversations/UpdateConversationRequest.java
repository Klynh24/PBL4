package pbl.backend.kchi.modules.messages.requests.conversations;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateConversationRequest {
    @NotBlank(message = "Không được để trống tên cuộc trò chuyện")
    private String name;
}