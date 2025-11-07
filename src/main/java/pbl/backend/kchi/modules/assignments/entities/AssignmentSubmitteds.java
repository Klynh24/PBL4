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
@Table(name = "assignment_submissions")
public class AssignmentSubmitteds {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="assignment_id", updatable = false)
    private Long assignmentId;

    @Column(name="user_id", updatable = false)
    private Long userId;

    private String fileUrl;

    private String score;

    @Column(name="submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onSubmitted() {
        submittedAt = LocalDateTime.now();
    }
}
