package pbl.backend.kchi.modules.refresh_tokens.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class Refresh_tokens {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    private String tokenHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreated() {
        LocalDateTime now = LocalDateTime.now();

        if(createdAt == null)
            createdAt = now;
        if(revokedAt == null)
            revokedAt = now;
        if(expiresAt == null)
            expiresAt = now.plusDays(1);
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getTokenHash() { return tokenHash; }

    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash;}

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }


}
