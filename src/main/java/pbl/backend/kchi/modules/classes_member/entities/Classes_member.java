package pbl.backend.kchi.modules.classes_member.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="classes_member")
public class Classes_member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long class_id;

    @Column(name="user_id", updatable = false)
    private Long userId;

    private String role;

    @Column(name="joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onJoined() {
        joinedAt = LocalDateTime.now();
    }

    public Long getClass_id() { return class_id;}

    public void setClass_id(Long class_id) { this.class_id = class_id; }

    public Long getUserId() { return userId;}

    public void setUserId(Long userId) { this.userId = userId;}

    public String getRole() { return role;}

    public void setRole(String role) { this.role = role;}

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

}
