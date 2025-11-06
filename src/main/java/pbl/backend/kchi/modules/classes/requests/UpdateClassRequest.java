package pbl.backend.kchi.modules.classes.requests;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class UpdateClassRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;

    private String description;

    private Long userId;

}
