package pbl.backend.kchi.modules.messages.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pbl.backend.kchi.modules.users.entities.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects; // Thêm
import java.util.Set;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name="created_at", updatable = false) // Sửa tên cột "create_at"
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;


    @PreUpdate
    protected void onUpdated(){
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreated() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "conversation_user",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )

    @JsonManagedReference("conversation-members")
    private Set<User> members = new HashSet<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference("conversation-messages")
    private Set<Messages> messages = new HashSet<>();
}