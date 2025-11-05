package pbl.backend.kchi.modules.users.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_catalogue_id", updatable = false)
    private Long userCatalogueid;
    private String name;
    private String email;
    private String phone;
    private String image;
    private String address;

    @JsonIgnore
    private String password;

    @Column(name="create_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name ="update_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreated() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdated() {
        updatedAt = LocalDateTime.now();
    }


    public User(
            String name,
            String email,
            String password,
            Long userCatalogueid,
            String phone

    ){
        this.name = name;
        this.email = email;
        this.password = password;
        this.userCatalogueid = userCatalogueid;
        this.phone = phone;
    }



}
