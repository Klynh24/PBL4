package pbl.backend.kchi.modules.users.repositories;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.users.entities.User;


import java.util.Optional;



@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @EntityGraph(attributePaths = "userCatalogues")
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

}
