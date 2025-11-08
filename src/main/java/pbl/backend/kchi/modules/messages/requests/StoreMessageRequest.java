package pbl.backend.kchi.modules.messages.requests;
import jakarta.validation.constraints.*;

import lombok.*;

@Data
public class StoreMessageRequest {

    private String text;

    @NotNull(message = "Phiên không được bỏ trống")
    private Long conversationId;

    private Long userId;

}
