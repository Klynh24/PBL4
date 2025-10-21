package pbl.backend.kchi.modules.messages.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", updatable = false)
    private Long sessionId;

    @Column(name="user_id", updatable = false)
    private Long userId;

    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    private String text;

    @PrePersist
    protected void onCreated() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id;}

    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSessionId() { return sessionId; }

    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getText() { return text; }

    public void setText(String text) { this.text = text;}

}
