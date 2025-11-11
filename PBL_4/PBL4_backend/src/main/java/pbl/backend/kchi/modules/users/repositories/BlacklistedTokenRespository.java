package pbl.backend.kchi.modules.users.repositories;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.users.entities.BlacklistedToken;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface BlacklistedTokenRespository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    int deleteByExpiryDateBefore(LocalDateTime currentDataTime);

}