package pbl.backend.kchi.modules.assignments.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.assignments.entities.AssignmentSubmission;
import pbl.backend.kchi.modules.assignments.mapper.SubmissionMapper;
import pbl.backend.kchi.modules.assignments.repositories.SubmittedRepository;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.StoreSubmittedRequest;
import pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions.UpdateSubmittedRequest;
import pbl.backend.kchi.modules.assignments.resources.SubmittedResource;
import pbl.backend.kchi.modules.assignments.services.interfaces.SubmittedServiceInterface;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.mapper.ClassMapper;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.resources.ClassResource;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

@Tag(name="API NỘP BÀI TẬP")
@Validated
@RestController
@RequestMapping("api/v1/submission")
public class SubmittedController extends BaseController<
        AssignmentSubmission,
        SubmittedResource,
        StoreSubmittedRequest,
        UpdateSubmittedRequest,
        SubmittedRepository
        > {
    public SubmittedController(
            SubmittedServiceInterface service,
            SubmissionMapper mapper,
            SubmittedRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.SUBMISSION);
    }


}