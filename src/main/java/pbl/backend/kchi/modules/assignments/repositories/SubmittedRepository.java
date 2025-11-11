package pbl.backend.kchi.modules.assignments.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmission;
import pbl.backend.kchi.modules.classes.entities.Classes;


public interface SubmittedRepository extends JpaRepository<AssignmentSubmission, Long>, JpaSpecificationExecutor<AssignmentSubmission> {

}