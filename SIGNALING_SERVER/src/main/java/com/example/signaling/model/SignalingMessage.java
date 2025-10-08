package com.example.signaling.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SignalingMessage {

    private final String type;
    private final String roomId;
    private final String sender;
    private final String target;
    private final Map<String, Object> payload;

    @JsonCreator
    public SignalingMessage(@JsonProperty("type") String type,
                            @JsonProperty("roomId") String roomId,
                            @JsonProperty("sender") String sender,
                            @JsonProperty("target") String target,
                            @JsonProperty("payload") Map<String, Object> payload) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.target = target;
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public String getType() {
        return type;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSender() {
        return sender;
    }

    public String getTarget() {
        return target;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public boolean isJoin() {
        return "join".equalsIgnoreCase(type);
    }

    public boolean isLeave() {
        return "leave".equalsIgnoreCase(type);
    }

    public boolean requiresRoom() {
        return !isJoin();
    }
}
