package pbl.backend.kchi.modules.classes.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;


import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.mapper.ClassMapper;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.resources.ClassResource;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;



@Tag(name="API Lớp")
@Validated
@RestController
@RequestMapping("api/v1/classes")
public class ClassController extends BaseController<
        Classes,
        ClassResource,
        StoreClassRequest,
        UpdateClassRequest,
        ClassRepository
        > {
    public ClassController(
            ClassServiceInterface service,
            ClassMapper mapper,
            ClassRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.CLASSES_USER);
    }


}