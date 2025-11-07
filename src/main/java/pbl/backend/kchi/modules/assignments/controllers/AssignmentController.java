package pbl.backend.kchi.modules.assignments.controllers;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.resources.AssignmentResource;
import pbl.backend.kchi.modules.assignments.services.interfaces.AssignmentServiceInterface;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.resources.ApiResource;

import java.util.Map;

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

    @GetMapping("assignments")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<Assignments> assignments = assignmentService.paginate(parameters);
        Page<AssignmentResource> assignmentResource = assignments.map(assignment ->
                AssignmentResource.builder()
                        .id(assignment.getId())
                        .title(assignment.getTitle())
                        .description(assignment.getDescription())
                        .dueDate(assignment.getDueDate())
                        .classId(assignment.getClassId())
                        .build()
        );

        ApiResource<Page<AssignmentResource>> response = ApiResource.ok(assignmentResource, "SUCCESS");

        logger.info("Method getUserCatalogues Running....");
        return ResponseEntity.ok(response);
    }

}
