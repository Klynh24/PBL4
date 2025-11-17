package pbl.backend.kchi.modules.users.controllers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.mappers.UserMapper;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

@Tag(name="API Thành viên")
@RestController
@RequestMapping("api/v1/users")
public class UserController extends BaseController<
        User,
        UserResource,
        StoreUserRequest,
        UpdateUserRequest,
        UserRepository
        > {

    @Autowired
    private UserRepository userRepository;


    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(
            UserServiceInterface service,
            UserMapper mapper,
            UserRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.USER);
    }


    @Operation(
            summary="Api Thông tin Thành viên",
            description = "Trả về thông tin của thành viên đang đăng nhập"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode="200",
                    description="Success",
                    content=@Content(schema = @Schema(implementation = ApiResource.class))
            ),
            @ApiResponse(
                    responseCode="403",
                    description="Không có quyền truy cập",
                    content=@Content(schema = @Schema(implementation = ApiResource.class))
            )
    })
    @Transactional
    @GetMapping("/me")
    public ResponseEntity<?> me(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User không tồn tại"));


        UserResource userResource = UserResource.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                // .users(user.getUserCatalogues())
                .build();

        ApiResource<UserResource> response = ApiResource.ok(userResource, "SUCCESS");
        logger.info("Success!");
        return ResponseEntity.ok(response);

    }

}
