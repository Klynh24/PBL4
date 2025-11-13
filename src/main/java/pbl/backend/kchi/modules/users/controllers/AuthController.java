package pbl.backend.kchi.modules.users.controllers;
//đăng nhập

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import pbl.backend.kchi.modules.refresh_tokens.requests.RefreshTokenRequest;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.resources.LoginResources;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.modules.users.requests.BlacklistedTokenRequest;
import pbl.backend.kchi.modules.users.services.impl.BlacklistService;
import pbl.backend.kchi.modules.users.resources.MessageResource;
import pbl.backend.kchi.services.JwtService;
import org.springframework.web.bind.annotation.RequestHeader;
import pbl.backend.kchi.modules.refresh_tokens.resources.RefreshTokenResource;
import pbl.backend.kchi.modules.refresh_tokens.repositories.RefreshtokensRepository;
import pbl.backend.kchi.modules.refresh_tokens.entities.Refresh_tokens;
import java.util.Optional;
import pbl.backend.kchi.resources.ApiResource;


@Tag(name="Auth Api")
@Validated
@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final UserServiceInterface userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private RefreshtokensRepository refreshTokenRepository;
    public AuthController(
            UserServiceInterface userService
    ) {
        this.userService = userService;

    }
    @PostMapping("login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Object result = userService.authenticate(request);

        if(result instanceof LoginResources loginResources) {
            ApiResource<LoginResources> response = ApiResource.ok(loginResources, "SUCCESS");
            return ResponseEntity.ok(response);
        }

        if(result instanceof ApiResource errorResource) {
            return ResponseEntity.unprocessableEntity().body(errorResource);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Network Error");


    }

    @PostMapping("blacklisted_tokens")
    public ResponseEntity<?> addTokenToBlacklist(@Valid @RequestBody BlacklistedTokenRequest request) {
        try {
 //           logger.info(request.getToken());

            Object result = blacklistService.create(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResource("Network error!"));
        }
    }
    @GetMapping("loggout")
    public ResponseEntity<?> loggout(@RequestHeader("Authorization") String bearerToken) {
        try {
            String token = bearerToken.substring(7);


            BlacklistedTokenRequest request = new BlacklistedTokenRequest();
            request.setToken(token);

            Object message = blacklistService.create(request);
            ApiResource<Void> Response = ApiResource.<Void>builder()
                    .success(true)
                    .message("Đăng xuất thành công!")
                    .status(HttpStatus.OK)
                    .build();
            return ResponseEntity.ok(Response);

        } catch (Exception e) {
            ApiResource<Void> errorResponse = ApiResource.<Void>builder()
                    .success(false)
                    .message("Network Error!")
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }

    }

    @Operation(summary = "Đăng ký tài khoản mới (Public)")
    @PostMapping("register")
    public ResponseEntity<?> register(@Valid @RequestBody StoreUserRequest request) {
        try {
            Object createdUser = userService.create(request);

            ApiResource<Object> response = ApiResource.ok(createdUser, "Đăng ký thành công!");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Lỗi đăng ký: {}", e.getMessage());
            return ResponseEntity.unprocessableEntity().body(ApiResource.error("REGISTRATION_ERROR", e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY));
        }
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if(!jwtService.isRefreshTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResource("Refresh Token không hợp lệ"));
        }

        Optional<Refresh_tokens> dbRefreshTokenOptional = refreshTokenRepository.findByRefreshToken(refreshToken);

        if(dbRefreshTokenOptional.isPresent()) {
            Refresh_tokens dbRefreshToken = dbRefreshTokenOptional.get();

            Long userId = dbRefreshToken.getUserId();
            String email = dbRefreshToken.getUser().getEmail();
            String newToken = jwtService.generateToken(userId, email, null);
            String newRefreshToken = jwtService.generateRefreshToken(userId, email);
            return ResponseEntity.ok(new RefreshTokenResource(newToken, newRefreshToken));

        }
        return ResponseEntity.internalServerError().body(new pbl.backend.kchi.resources.MessageResource("Network Error!"));

    }



}
