package pbl.backend.kchi.modules.assignments.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.services.interfaces.AssignmentServiceInterface;
import pbl.backend.kchi.modules.assignments.repositories.AssignmentRepository;
import pbl.backend.kchi.services.BaseService;

@Service
public class AssignmentService extends BaseService implements AssignmentServiceInterface {
    @Autowired
    private AssignmentRepository assignmentRespository;

    @Override
    @Transactional
    public Assignments create(StoreAssignmentRequest request) {
        try {
            Assignments payload = Assignments.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .classId(request.getClassId())
                    .dueDate(request.getDueDate())
                    .build();
            return assignmentRespository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }

    }
}
