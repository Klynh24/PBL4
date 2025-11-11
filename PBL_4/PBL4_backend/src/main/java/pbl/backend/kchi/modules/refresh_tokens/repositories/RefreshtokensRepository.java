package pbl.backend.kchi.modules.refresh_tokens.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pbl.backend.kchi.modules.refresh_tokens.entities.Refresh_tokens;



@Repository
public interface RefreshtokensRepository  extends JpaRepository<Refresh_tokens, Long>{
    boolean existsByRefreshToken(String refreshToken);
    Optional<Refresh_tokens> findByRefreshToken(String refreshToken);
    Optional<Refresh_tokens> findByUserId(Long userId);
    int deleteByExpiryDateBefore(LocalDateTime currentDataTime);


}