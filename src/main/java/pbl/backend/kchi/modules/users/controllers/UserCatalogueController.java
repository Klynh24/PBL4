package pbl.backend.kchi.modules.users.controllers;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;

import pbl.backend.kchi.modules.users.mappers.UserCatalogueMapper;
import pbl.backend.kchi.modules.users.repositories.UserCatalogueRepository;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;


//phân quyền
@Tag(name="API Nhóm thành viên")
@Validated
@RestController
@RequestMapping("api/v1/user_catalogues")
public class UserCatalogueController extends BaseController<
        UserCatalogue,
        UserCatalogueResource,
        StoreRequest,
        UpdateRequest,
        UserCatalogueRepository
        > {
    public UserCatalogueController(
            UserCatalogueServiceInterface service,
            UserCatalogueMapper mapper,
            UserCatalogueRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.USER_CATALOGUE);
    }


}