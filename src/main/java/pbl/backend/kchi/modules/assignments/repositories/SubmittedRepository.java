package pbl.backend.kchi.modules.assignments.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmitteds;


public interface SubmittedRepository extends JpaRepository<AssignmentSubmitteds, Long> {
}