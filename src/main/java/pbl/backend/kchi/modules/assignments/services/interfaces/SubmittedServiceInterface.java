package pbl.backend.kchi.modules.assignments.services.interfaces;

import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmitteds;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;

public interface SubmittedServiceInterface {
    AssignmentSubmitteds create(Long userId, StoreSubmittedRequest request);

}
