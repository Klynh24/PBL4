package pbl.backend.kchi.modules.messages.requests.conversations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class StoreConversationRequest {
    private String name;

}
