package pbl.backend.kchi.modules.assignments.controllers;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.resources.AssignmentResource;
import pbl.backend.kchi.modules.assignments.services.interfaces.AssignmentServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

@Validated
@RestController
@RequestMapping("api/v1")
public class AssignmentController {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);
    private final AssignmentServiceInterface assignmentService;

    public AssignmentController(
            AssignmentServiceInterface assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/assignments")
    public ResponseEntity<?> store(@Valid @RequestBody StoreAssignmentRequest request) {
        Assignments assignments = assignmentService.create(request);
        AssignmentResource assignmentResource = AssignmentResource.builder()
                .id(assignments.getId())
                .title(assignments.getTitle())
                .description(assignments.getDescription())
                .classId(assignments.getClassId())
                .dueDate(assignments.getDueDate())
                .build();
        ApiResource<AssignmentResource> response = ApiResource.ok(assignmentResource, "Thêm mới bản ghi thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);
    }

}
