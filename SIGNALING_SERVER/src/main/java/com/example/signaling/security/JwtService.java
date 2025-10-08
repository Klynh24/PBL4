package com.example.signaling.security;

import java.util.Optional;

public interface JwtService {
    Optional<ClientPrincipal> parse(String bearerToken);
}
