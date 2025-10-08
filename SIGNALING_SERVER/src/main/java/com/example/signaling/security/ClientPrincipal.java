package com.example.signaling.security;

import java.util.Map;

public record ClientPrincipal(String subject, Map<String, Object> claims) {
    public static final String ATTRIBUTE_KEY = "clientPrincipal";

    public ClientPrincipal {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }
}
