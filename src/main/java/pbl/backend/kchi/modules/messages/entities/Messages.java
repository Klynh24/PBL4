package pbl.backend.kchi.modules.messages.entities;

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
@Table(name = "messages")
public class Messages {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="conversation_id", updatable = false)
    private Long conversationId;

    @Column(name="user_id", updatable = false)
    private Long userId;

    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    private String text;

    @PrePersist
    protected void onCreated() { createdAt = LocalDateTime.now(); }

}
