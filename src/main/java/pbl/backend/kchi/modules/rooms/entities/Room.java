package pbl.backend.kchi.modules.rooms.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.*;
import lombok.NoArgsConstructor;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.users.entities.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    @JsonBackReference("class-rooms")
    private Classes classes;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_participants",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonManagedReference("room-participants")
    private Set<User> participants = new HashSet<>();

    private String name;

    @Builder.Default 
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name="create_at", updatable=false)
    private LocalDateTime createdAt;

    @Column(name="update_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreated(){
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdated(){
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

