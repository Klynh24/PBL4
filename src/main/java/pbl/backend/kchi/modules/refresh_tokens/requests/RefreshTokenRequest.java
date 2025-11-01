package pbl.backend.kchi.modules.refresh_tokens.requests;
import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "RefreshToken không được để trống")
    private String refreshToken;

}
