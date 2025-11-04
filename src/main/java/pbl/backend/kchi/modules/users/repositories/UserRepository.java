package pbl.backend.kchi.modules.users.repositories;


import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.users.entities.User;


import java.util.Optional;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
