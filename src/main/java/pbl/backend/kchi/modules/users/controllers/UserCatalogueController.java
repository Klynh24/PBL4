package pbl.backend.kchi.modules.users.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;

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
@Validated
@RestController
@RequestMapping("api/v1")
public class UserCatalogueController {

    @Autowired
    private UserCataloguesRespository userCataloguesRespository;

    private static final Logger logger = LoggerFactory.getLogger(UserCatalogueController.class);
    private final UserCatalogueServiceInterface userCatalogueService;


    public UserCatalogueController(
            UserCatalogueServiceInterface userCatalogueService
    ) {
        this.userCatalogueService = userCatalogueService;

    }

    @GetMapping("user_catalogues")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<UserCatalogue> userCatalogues = userCatalogueService.paginate(parameters);
        Page<UserCatalogueResource> userCatalogueResource = userCatalogues.map(userCatalogue ->
                UserCatalogueResource.builder()
                        .id(userCatalogue.getId())
                        .name(userCatalogue.getName())
                        .publish(userCatalogue.getPublish())
                        .build()
                );

        ApiResource<Page<UserCatalogueResource>> response = ApiResource.ok(userCatalogueResource, "SUCCESS");

        logger.info("Method getUserCatalogues Running....");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user_catalogues")
    public ResponseEntity<?> store(@Valid @RequestBody StoreRequest request) {
        logger.info("Method Store Running....");
        try {
            UserCatalogue userCatalogue = userCatalogueService.create(request);
            UserCatalogueResource userCatalogueResource = UserCatalogueResource.builder()
                    .id(userCatalogue.getId())
                    .name(userCatalogue.getName())
                    .publish(userCatalogue.getPublish())
                    .build();
            ApiResource<UserCatalogueResource> response = ApiResource.ok(userCatalogueResource, "Thêm mới bản ghi thành công");
            return ResponseEntity.ok(response);
       } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND",e.getMessage(),HttpStatus.BAD_REQUEST)
            );

        }


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

    @GetMapping("/user_catalogues/{id}")
    public ResponseEntity<?> show(@PathVariable Long id) {
        UserCatalogue userCatalogue = userCataloguesRespository.findById(id).orElseThrow(() -> new RuntimeException("Bản ghi không tồn tại"));
        UserCatalogueResource userCatalogueResource = UserCatalogueResource.builder()
                .id(userCatalogue.getId())
                .name(userCatalogue.getName())
                .publish(userCatalogue.getPublish())
                .build();
        ApiResource<UserCatalogueResource> response = ApiResource.ok(userCatalogueResource, "SUCCESS");
        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/user_catalogues/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userCatalogueService.delete(id);
            return ResponseEntity.ok(ApiResource.message("Xóa bản ghi thành công", HttpStatus.OK));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND",e.getMessage(),HttpStatus.BAD_REQUEST)
            );

        } catch (Exception e) {
            String message = "Có lỗi xảy ra trong quá trình xử lí" + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", message,
                            HttpStatus.INTERNAL_SERVER_ERROR)

            );
        }
    }

    @DeleteMapping("/user_catalogues")
    public ResponseEntity<?> deleteMany(@RequestBody List<Long> Ids) {
        logger.info("method deleteMultipeEntity is running....");
        try {
            userCatalogueService.deleteMultipleEntity(Ids);
            return ResponseEntity.ok(ApiResource.message("Xóa bản ghi thành công", HttpStatus.OK));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND",e.getMessage(),HttpStatus.BAD_REQUEST)
            );

        }
        catch (Exception e) {
            String message = "Có lỗi xảy ra trong quá trình xử lí" + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", message,
                            HttpStatus.INTERNAL_SERVER_ERROR)

            );

        }
    }

}
