package pbl.backend.kchi.modules.assignments.requests;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
public class StoreAssignmentRequest {
    @NotBlank(message = "Tên không được để trống")
    private String title;

    @NotNull(message = "Tên lớp không được bỏ trống")
    private Long classId;

    private String description;

    private LocalDateTime dueDate;


}
