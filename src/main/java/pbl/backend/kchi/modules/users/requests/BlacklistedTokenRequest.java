package pbl.backend.kchi.modules.users.requests;
import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
public class BlacklistedTokenRequest {

    @NotBlank(message = "Token không được để trống")
    private String token;

}
