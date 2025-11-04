package pbl.backend.kchi.modules.users.controllers;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.services.impl.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

@RestController
@RequestMapping("api/v1")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static Logger logger = LoggerFactory.getLogger(UserService.class);
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

}
