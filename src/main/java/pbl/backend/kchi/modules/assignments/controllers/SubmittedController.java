package pbl.backend.kchi.modules.assignments.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmitteds;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;
import pbl.backend.kchi.modules.assignments.resources.SubmittedResource;
import pbl.backend.kchi.modules.assignments.services.interfaces.SubmittedServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

@Validated
@RestController
@RequestMapping("api/v1")
public class SubmittedController {
    private static final Logger logger = LoggerFactory.getLogger(SubmittedController.class);
    private final SubmittedServiceInterface submittedService;

    public SubmittedController(
            SubmittedServiceInterface submittedService
    ) {
        this.submittedService = submittedService;

    }

    @PostMapping("/submitted/{id}")
    public ResponseEntity<?> store(
            @PathVariable Long id,
            @Valid @RequestBody StoreSubmittedRequest request) {
        AssignmentSubmitteds submitted = submittedService.create(id, request);

        SubmittedResource submittedResource = SubmittedResource.builder()
                .id(submitted.getId())
                .fileUrl(submitted.getFileUrl())
                .userId(id)
                .score(submitted.getScore())
                .assignmentId(submitted.getAssignmentId())
                .build();
        ApiResource<SubmittedResource> response = ApiResource.ok(submittedResource, "Nộp bài thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);

    }

}
