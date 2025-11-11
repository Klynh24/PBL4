package pbl.backend.kchi.modules.assignments.entities;

import com.fasterxml.jackson.annotation.JsonBackReference; // Thêm
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pbl.backend.kchi.modules.users.entities.User; // Thêm

import java.time.LocalDateTime;
import java.util.Objects; // Thêm

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "assignment_submissions")

public class AssignmentSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", updatable = false)
    @JsonBackReference("assignment-submissions")
    private Assignments assignment;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    @JsonBackReference("user-submissions")
    private User user;


    private String fileUrl;

    private String score;


    @Column(name="submitted_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at") // Thêm từ mẫu
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreated() {
        createdAt = LocalDateTime.now();
    }


    @PreUpdate
    protected void onUpdated() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentSubmission that = (AssignmentSubmission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}