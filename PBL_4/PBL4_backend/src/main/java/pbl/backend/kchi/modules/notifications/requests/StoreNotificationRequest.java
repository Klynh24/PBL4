package pbl.backend.kchi.modules.notifications.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class StoreNotificationRequest {
    @NotBlank(message = "Thông báo không được để trống")
    private String message;

    @NotBlank(message = "Loại thông báo không được để trống")
    private String type;

    @NotNull(message = "Người nhận không được bỏ trống")
    private Long userId;


}