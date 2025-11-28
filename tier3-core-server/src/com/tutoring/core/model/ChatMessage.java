package com.tutoring.core.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single chat message with timestamp
 */
public class ChatMessage {
    private final String username;
    private final String message;
    private final LocalDateTime timestamp;
    
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    public ChatMessage(String username, String message) {
        this.username = username;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTime() {
        return timestamp.format(TIME_FORMATTER);
    }
    
    /**
     * Format message for transmission: CHAT:username:message
     */
    public String toProtocolString() {
        return "CHAT:" + username + ":" + message;
    }
    
    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + username + ": " + message;
    }
}

