package pbl.backend.kchi.modules.notifications.requests;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateNotificationRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean readStatus;


}
