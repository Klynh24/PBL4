package pbl.backend.kchi.modules.rooms.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreRoomRequest {

    @NotNull(message = "ID Lớp học không được để trống")
    private Long classId;

    @NotNull(message = "ID Người tạo không được để trống")
    private Long userId;

    @NotBlank(message = "Tên phiên không được để trống")
    private String name;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}