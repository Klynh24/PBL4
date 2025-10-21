package pbl.backend.kchi.modules.classes.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "classes")
public class Classes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", updatable = false)
    private Long userid;
    private String name;
    private String description;

    @Column(name="created_at", updatable = false)
    private LocalDateTime createAt;

    @PrePersist
    protected void onCreate() {
        createAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getUserid() { return userid; }

    public void setUserid(Long userid) { this.userid = userid; }

    public String getName() { return name;}

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription() { this.description = description;}

    public LocalDateTime getCreatedAt() {
        return createAt;
    }



}
