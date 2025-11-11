package pbl.backend.kchi.modules.assignments.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.assignments.entities.Assignments;
import pbl.backend.kchi.modules.assignments.mapper.AssignmentMapper;
import pbl.backend.kchi.modules.assignments.repositories.AssignmentRepository;
import pbl.backend.kchi.modules.assignments.requests.StoreAssignmentRequest;
import pbl.backend.kchi.modules.assignments.requests.UpdateAssignmentRequest;

import pbl.backend.kchi.modules.assignments.resources.AssignmentResource;
import pbl.backend.kchi.modules.assignments.services.interfaces.AssignmentServiceInterface;

@Tag(name="API BÀI TẬP")
@Validated
@RestController
@RequestMapping("api/v1/assignments")
public class AssignmentController extends BaseController<
        Assignments,
        AssignmentResource,
        StoreAssignmentRequest,
        UpdateAssignmentRequest,
        AssignmentRepository
        > {
    public AssignmentController(
            AssignmentServiceInterface service,
            AssignmentMapper mapper,
            AssignmentRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.ASSIGNMENT);
    }


}