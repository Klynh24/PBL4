package pbl.backend.kchi.modules.assignments.services.interfaces;

import org.springframework.data.domain.Page;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;

import java.util.Map;


public interface AssignmentServiceInterface {
    Assignments create(StoreAssignmentRequest request);
    Page<Assignments> paginate(Map<String, String[]> parameters );


}
