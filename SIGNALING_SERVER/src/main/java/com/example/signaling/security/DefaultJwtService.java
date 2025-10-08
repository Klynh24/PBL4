package com.example.signaling.security;

import com.example.signaling.config.SignalingProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "signaling.auth", name = "enabled", havingValue = "true")
public class DefaultJwtService implements JwtService {

    private final SignalingProperties properties;
    private SecretKey secretKey;

    public DefaultJwtService(SignalingProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String secret = properties.getAuth().getJwtSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<ClientPrincipal> parse(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            return Optional.empty();
        }
        String token = bearerToken.replaceFirst("(?i)^Bearer\\s+", "");
        try {
            Claims claims = Jwts.parserBuilder()
                    .requireIssuer(properties.getAuth().getIssuer())
                    .setAllowedClockSkewSeconds(Duration.ofSeconds(properties.getAuth().getClockSkewSeconds()).getSeconds())
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            Map<String, Object> copyClaims = claims.entrySet().stream()
                    .filter(entry -> !Claims.SUBJECT.equals(entry.getKey()))
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            return Optional.of(new ClientPrincipal(subject, copyClaims));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }
}
