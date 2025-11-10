package pbl.backend.kchi.modules.users.controllers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;

import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.modules.users.repositories.UserRepository;

import org.springframework.security.core.context.SecurityContextHolder;

import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserServiceInterface userService;

    public UserController(
            UserServiceInterface userService
    ) {
        this.userService = userService;

    }


    @GetMapping("/me")
    public ResponseEntity<?> me() {
        // String email = "tuitentoan3004@gmai.com";
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        logger.info(email);

        User user = userRepository.findByEmail(email).orElseThrow(()->new RuntimeException("Người dùng không tồn tại!"));

        UserResource userResource = UserResource.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .build();

        ApiResource<UserResource> response = ApiResource.ok(userResource, "SUCCESS");

        logger.info("SUCCESS!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<?> store(@Valid @RequestBody StoreUserRequest request) {
        User user = userService.create(request);
        UserResource userResource = UserResource.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .image(user.getImage())
                .email(user.getEmail())
                .userCatalogueId(user.getUserCatalogueid())
                .build();
        ApiResource<UserResource> response = ApiResource.ok(userResource, "Thêm mới bản ghi thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        logger.info("Method Store Running....");
        try {
            User user = userService.update(id, request);
            UserResource userResource = UserResource.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .image(user.getImage())
                    .email(user.getEmail())
                    .userCatalogueId(user.getUserCatalogueid())
                    .build();
            ApiResource<UserResource> response = ApiResource.ok(userResource, "Cập nhật bản ghi thành công");

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

    @GetMapping("users")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<User> users = userService.paginate(parameters);
        Page<UserResource> userResource = users.map(user ->
                UserResource.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .phone(user.getPhone())
                        .address(user.getAddress())
                        .image(user.getImage())
                        .email(user.getEmail())
                        .userCatalogueId(user.getUserCatalogueid())
                        .build()

        );

        ApiResource<Page<UserResource>> response = ApiResource.ok(userResource, "SUCCESS");

        logger.info("Method getUserCatalogues Running....");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userService.delete(id);
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

}