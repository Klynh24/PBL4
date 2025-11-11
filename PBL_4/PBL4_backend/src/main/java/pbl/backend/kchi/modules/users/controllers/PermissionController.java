package pbl.backend.kchi.modules.users.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.users.entities.Permission;
import pbl.backend.kchi.modules.users.mappers.PermissionMapper;
import pbl.backend.kchi.modules.users.repositories.PermissionRepository;
import pbl.backend.kchi.modules.users.requests.Permission.StoreRequest;
import pbl.backend.kchi.modules.users.requests.Permission.UpdateRequest;
import pbl.backend.kchi.modules.users.resources.PermissionResource;
import pbl.backend.kchi.modules.users.services.interfaces.PermissionServiceInterface;

@Tag(name="Permission Api")
@Validated
@RestController
@RequestMapping("api/v1/permissions")
public class PermissionController extends BaseController<
        Permission,
        PermissionResource,
        StoreRequest,
        UpdateRequest,
        PermissionRepository
        > {
    public PermissionController(
            PermissionServiceInterface service,
            PermissionMapper mapper,
            PermissionRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.PERMISSION);
    }
}