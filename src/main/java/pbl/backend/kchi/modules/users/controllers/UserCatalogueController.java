package pbl.backend.kchi.modules.users.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;

import javax.persistence.EntityNotFoundException;

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
    public ResponseEntity<?> store(@Valid @RequestBody StoreRequest request) {
        UserCatalogue userCatalogue = userCatalogueService.create(request);
        UserCatalogueResource userCatalogueResource = UserCatalogueResource.builder()
                .id(userCatalogue.getId())
                .name(userCatalogue.getName())
                .publish(userCatalogue.getPublish())
                .build();
        ApiResource<UserCatalogueResource> response = ApiResource.ok(userCatalogueResource, "Thêm mới bản ghi thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user_catalogues/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequest request
    ) {
        logger.info("Method Store Running....");
        try {
            UserCatalogue userCatalogue = userCatalogueService.update(id, request);
            UserCatalogueResource userCatalogueResource = UserCatalogueResource.builder()
                    .id(userCatalogue.getId())
                    .name(userCatalogue.getName())
                    .publish(userCatalogue.getPublish())
                    .build();
            ApiResource<UserCatalogueResource> response = ApiResource.ok(userCatalogueResource, "Cập nhật bản ghi thành công");

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
              ApiResource.error("NOT_FOUND",e.getMessage(),HttpStatus.BAD_REQUEST)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR","Có lỗi xảy ra trong quá trình cập nhật",
                            HttpStatus.INTERNAL_SERVER_ERROR)

            );
        }
    }

}
