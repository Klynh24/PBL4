package pbl.backend.kchi.modules.users.entities;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmission;

import java.util.HashSet;

import java.time.LocalDateTime;
import java.util.Objects;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_catalogue_user",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "user_catalogue_id")
    )
    @JsonManagedReference
    private Set<UserCatalogue> userCatalogues = new HashSet<>();


    @Builder.Default
    @OneToMany(
            mappedBy = "user_id",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference("user-submissions") // Khớp tên
    private Set<AssignmentSubmission> submissions = new HashSet<>();

    private String name;
    private String email;
    private String password;
    private String phone;
    private String image;
    private String address;

    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreated(){
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdated(){
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User that = (User) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}