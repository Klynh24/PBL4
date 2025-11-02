package pbl.backend.kchi.modules.users.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;

//phân quyền
@Validated
@RestController
@RequestMapping("api/v1")
public class UserCatalogueController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserCatalogueServiceInterface userCatalogueService;
    public UserCatalogueController(
            UserCatalogueServiceInterface userCatalogueService
    ) {
        this.userCatalogueService = userCatalogueService;

    }

    @PostMapping("/user_catalogues")
    public ResponseEntity<?> create(@Valid @RequestBody StoreRequest request) {
        logger.info("Method Running....");

        return ResponseEntity.ok(null);
    }

}
