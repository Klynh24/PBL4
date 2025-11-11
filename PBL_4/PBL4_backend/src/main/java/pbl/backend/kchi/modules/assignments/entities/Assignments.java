package pbl.backend.kchi.modules.assignments.entities;

import com.fasterxml.jackson.annotation.JsonBackReference; // Thêm
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pbl.backend.kchi.modules.classes.entities.Classes; // Thêm

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects; // Thêm
import java.util.Set;

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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", updatable = false)
    @JsonBackReference("class-assignments")
    private Classes classes;


    @Builder.Default
    @OneToMany(
            mappedBy = "assignment",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )

    @JsonManagedReference("assignment-submissions")
    private Set<AssignmentSubmission> submissions = new HashSet<>();

    private String title;

    private String description;

    @Column(name="create_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name="due_date", updatable = false)
    private LocalDateTime dueDate;




    @PrePersist
    protected void onCreated() {
        createdAt = LocalDateTime.now();
    }

    @Column(name="update_at")
    private LocalDateTime updatedAt;


    @PreUpdate
    protected void onUpdated(){
        updatedAt = LocalDateTime.now();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Assignments that = (Assignments) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}