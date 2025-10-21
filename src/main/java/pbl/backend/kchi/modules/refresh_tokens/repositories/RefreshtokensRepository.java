package pbl.backend.kchi.modules.refresh_tokens.repositories;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.refresh_tokens.entities.Refresh_tokens;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;



@Repository
public interface RefreshtokensRepository  extends JpaRepository<Refresh_tokens, Long>{
}
