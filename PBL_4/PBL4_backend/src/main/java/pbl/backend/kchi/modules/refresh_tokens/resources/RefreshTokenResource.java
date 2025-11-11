package pbl.backend.kchi.modules.refresh_tokens.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class RefreshTokenResource {
    private final String token;
    private String refreshToken;

}