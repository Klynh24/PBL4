package pbl.backend.kchi.modules.messages.requests.conversations;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class StoreConversationRequest {
    @NotBlank(message = "Không đuo")
    private String name;

}
