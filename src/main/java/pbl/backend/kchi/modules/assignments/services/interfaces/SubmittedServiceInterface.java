package pbl.backend.kchi.modules.assignments.services.interfaces;

import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmission;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.UpdateSubmittedRequest;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;

public interface SubmittedServiceInterface extends BaseServiceInterface<AssignmentSubmission, StoreSubmittedRequest, UpdateSubmittedRequest> {

}
