package pbl.backend.kchi.modules.class_invites.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_invites")
public class Class_invites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", updatable = false)
    private Long classId;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    private String token;
    private Role role;

    @Column(name = "expires_at")
    public LocalDateTime expiresAt;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "used_at")
    public LocalDateTime usedAt;

    @PrePersist
    protected void onCreated() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }
        if (expiresAt == null) {
            expiresAt = now.plusDays(1); //token het han sau 1 ngày
        }
    }

    @PreUpdate
    protected void onUsed() { usedAt = LocalDateTime.now(); }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getClassId() { return classId; }

    public void setClassId(Long classId) { this.classId = classId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public Role getRole() { return role;}

    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }

    public LocalDateTime getUsedAt() { return usedAt;}


}
