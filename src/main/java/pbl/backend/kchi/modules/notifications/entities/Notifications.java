package pbl.backend.kchi.modules.notifications.entities;

import com.fasterxml.jackson.annotation.JsonBackReference; // Thêm
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pbl.backend.kchi.modules.users.entities.User; // Thêm

import java.time.LocalDateTime;
import java.util.Objects; // Thêm

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "notifications")
public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    @JsonBackReference("user-notifications")
    private User user;

    private String message;
    private String type;

    @Column(name = "read_status", nullable = false)
    @Builder.Default
    private Boolean readStatus = false;


    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreated() {
        createdAt = LocalDateTime.now();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notifications that = (Notifications) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}