package pbl.backend.kchi.modules.users.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.services.impl.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import pbl.backend.kchi.modules.users.resources.ApiResource;

@RestController
@RequestMapping("api/v1")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private static Logger logger = LoggerFactory.getLogger(UserService.class);

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

}
