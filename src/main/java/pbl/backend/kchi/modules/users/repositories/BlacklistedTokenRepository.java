package pbl.backend.kchi.modules.users.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.users.entities.BlacklistedToken;


import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    int deleteByExpiryDateBefore(LocalDateTime currentDataTime);

}
