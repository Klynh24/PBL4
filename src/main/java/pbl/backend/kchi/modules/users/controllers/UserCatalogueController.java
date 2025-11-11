package pbl.backend.kchi.modules.users.controllers;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;

import pbl.backend.kchi.modules.users.mappers.UserCatalogueMapper;
import pbl.backend.kchi.modules.users.repositories.UserCataloguesRespository;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;


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
        UserCataloguesRespository
        > {
    public UserCatalogueController(
            UserCatalogueServiceInterface service,
            UserCatalogueMapper mapper,
            UserCataloguesRespository repo
    ){
        super(service, mapper, repo, PermissionEnum.USER_CATALOGUE);
    }


}