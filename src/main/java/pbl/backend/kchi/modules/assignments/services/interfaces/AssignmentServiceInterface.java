package pbl.backend.kchi.modules.assignments.services.interfaces;

import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;



public interface AssignmentServiceInterface {
    Assignments create(StoreAssignmentRequest request);

}
