package pbl.backend.kchi.modules.classes.requests;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoreClassRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;

    private String description;

    private Long userId;
    private List<Long> members;

}