package pbl.backend.kchi.modules.assignments.services.interfaces;

import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.requests.UpdateAssignmentRequest;

import pbl.backend.kchi.services.interfaces.BaseServiceInterface;




public interface AssignmentServiceInterface extends BaseServiceInterface<Assignments, StoreAssignmentRequest, UpdateAssignmentRequest> {

}