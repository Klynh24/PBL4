package pbl.backend.kchi.modules.classes.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.classes.entities.Classes;

import java.util.Optional;


@Repository
public interface ClassRepository extends JpaRepository<Classes, Long>, JpaSpecificationExecutor<Classes> {
    @Query("""
        SELECT DISTINCT c FROM Classes c 
        LEFT JOIN FETCH c.user 
        LEFT JOIN c.members m
        WHERE c.user.id = :userId OR m.id = :userId
    """)
    Page<Classes> findByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Classes> findByCode(String code);

}