package pbl.backend.kchi.modules.users.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pbl.backend.kchi.modules.users.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    @EntityGraph(attributePaths = "userCatalogues")
    Optional<User> findByEmail(String email);

    // FIX: Thêm các import Page, Pageable, Specification ở trên
    @Override
    @EntityGraph(attributePaths = "userCatalogues")
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    // FIX: Xoá bỏ dòng trùng lặp existsByEmail
    Boolean existsByEmail(String email);
}