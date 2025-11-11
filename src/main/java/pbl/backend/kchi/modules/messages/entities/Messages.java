package pbl.backend.kchi.modules.messages.entities;

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
@Table(name = "messages")
public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", updatable = false)
    @JsonBackReference("conversation-messages")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    @JsonBackReference("user-messages")
    private User user;

    private String text;

    @Column(name="created_at", updatable = false)
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
        Messages message = (Messages) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}