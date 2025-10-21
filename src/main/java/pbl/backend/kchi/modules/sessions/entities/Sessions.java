package pbl.backend.kchi.modules.sessions.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Sessions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="class_id", updatable = false)
    private Long classId;
    private String title;

    @Column(name = "scheduled_at", updatable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreated() {
        LocalDateTime now = LocalDateTime.now();

        if(createdAt == null)
            createdAt = now;
        if(scheduledAt == null)
            scheduledAt = now;
        if(startedAt == null)
            startedAt = now;
        if(endedAt == null)
            endedAt = now;
    }



    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getClassId() { return classId; }

    public void setClassId(Long classId) { this.classId = classId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }

    public LocalDateTime getStartedAt() { return startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }



}

