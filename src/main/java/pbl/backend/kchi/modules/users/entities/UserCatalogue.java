package pbl.backend.kchi.modules.users.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "users_catalogues")
public class UserCatalogue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publish", nullable = false, columnDefinition = "TINYINT")
    private Integer publish;

    @Column(name="create_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name ="update_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreated() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdated() {
        updatedAt = LocalDateTime.now();
    }
}
