package com.example.signaling.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "signaling")
public class SignalingProperties {

    @Min(1024)
    private int maxPayloadBytes = 64 * 1024;
    private final Auth auth = new Auth();
    private final Redis redis = new Redis();
    private final Websocket websocket = new Websocket();

    @Getter
    @Setter
    public static class Auth {
        private boolean enabled = false;
        private String jwtSecret = "change-me";
        private String issuer = "signaling-server";
        private long clockSkewSeconds = 60;
    }

    @Getter
    @Setter
    public static class Redis {
        private boolean enabled = false;
        private String channelPrefix = "signaling.room.";
    }

    @Getter
    @Setter
    public static class Websocket {
        private String path = "/ws";
        private String[] allowedOrigins = new String[]{"*"};
        private boolean allowSockJs = false;
    }
}
