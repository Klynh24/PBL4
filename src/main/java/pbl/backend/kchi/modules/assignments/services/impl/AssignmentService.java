package pbl.backend.kchi.modules.assignments.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.services.interfaces.AssignmentServiceInterface;
import pbl.backend.kchi.modules.assignments.repositories.AssignmentRepository;
import pbl.backend.kchi.services.BaseService;

import java.util.Map;

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

    @Override
    public Page<Assignments> paginate(Map<String, String[]> parameters) {
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page")[0]) : 1;
        int perpage = parameters.containsKey("perpage") ? Integer.parseInt(parameters.get("perpage")[0]) : 20;
        String sortParam = parameters.containsKey("sort") ? parameters.get("sort")[0] : null;
        Sort sort = createSort(sortParam);
        Pageable pageable = PageRequest.of(page - 1, perpage, sort);
        return assignmentRespository.findAll(pageable);

    }
}
