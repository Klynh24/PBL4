package pbl.backend.kchi.modules.users.repositories;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;


import java.util.Optional;

@Repository
public interface UserCataloguesRespository extends JpaRepository<UserCatalogue, Long>{

}
