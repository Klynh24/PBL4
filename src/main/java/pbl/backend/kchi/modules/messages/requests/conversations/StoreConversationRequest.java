package pbl.backend.kchi.modules.messages.requests.conversations;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class StoreConversationRequest {
    @NotBlank(message = "Không được để trống tên cuộc trò chuyện")
    private String name;

}
