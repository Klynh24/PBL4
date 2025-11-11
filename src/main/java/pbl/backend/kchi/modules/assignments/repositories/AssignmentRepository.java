package pbl.backend.kchi.modules.assignments.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pbl.backend.kchi.modules.assignments.entities.Assignments;



public interface AssignmentRepository extends JpaRepository<Assignments, Long>, JpaSpecificationExecutor<Assignments> {

}
