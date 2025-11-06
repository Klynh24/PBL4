package pbl.backend.kchi.modules.classes.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.resources.ClassResource;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;
import pbl.backend.kchi.modules.users.controllers.UserCatalogueController;

import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.resources.ApiResource;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

@Validated
@RestController
@RequestMapping("api/v1")
public class ClassController {
    private static final Logger logger = LoggerFactory.getLogger(UserCatalogueController.class);
    private final ClassServiceInterface classService;

    public ClassController(
            ClassServiceInterface classService
    ) {
        this.classService = classService;

    }

    @PostMapping("/classes/{id}")
    public ResponseEntity<?> store(
            @PathVariable Long id,
            @Valid @RequestBody StoreClassRequest request) {
        Classes classes = classService.create(id, request);

            ClassResource classResource = ClassResource.builder()
                    .id(classes.getId())
                    .name(classes.getName())
                    .userId(id)
                    .description(classes.getDescription())
                    .build();
            ApiResource<ClassResource> response = ApiResource.ok(classResource, "Thêm mới bản ghi thành công");
            logger.info("Method Store Running....");
            return ResponseEntity.ok(response);

    }

    @PutMapping("/classes/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request
    ) {
        logger.info("Method Store Running....");
        try {
            Classes classes = classService.update(id, request);
            ClassResource classResource = ClassResource.builder()
                    .name(classes.getName())
                    .description(classes.getDescription())
                    .build();
            ApiResource<ClassResource> response = ApiResource.ok(classResource, "Cập nhật bản ghi thành công");

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

    @GetMapping("classes")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<Classes> classess = classService.paginate(parameters);
        Page<ClassResource> classResource = classess.map(classes ->
                ClassResource.builder()
                        .id(classes.getId())
                        .name(classes.getName())
                        .description(classes.getDescription())
                        .build()
        );

        ApiResource<Page<ClassResource>> response = ApiResource.ok(classResource, "SUCCESS");

        logger.info("Method getClasses Running....");
        return ResponseEntity.ok(response);
    }


}
