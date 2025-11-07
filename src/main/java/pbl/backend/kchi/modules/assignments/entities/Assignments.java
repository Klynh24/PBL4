package pbl.backend.kchi.modules.assignments.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "assignments")
public class Assignments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="class_id", updatable = false)
    private Long classId;

    private String title;

    private String description;

    @Column(name="create_at", updatable = false)
    private LocalDateTime createAt;

    @Column(name="due_date", updatable = false)
    private LocalDateTime dueDate;

    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
    }
}
