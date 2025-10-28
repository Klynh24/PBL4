package pbl.backend.kchi.modules.users.controllers;
//đăng nhập

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.resources.LoginResources;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.resources.ErrorResource;
import pbl.backend.kchi.modules.users.requests.BlacklistedTokenRequest;
import pbl.backend.kchi.modules.users.services.impl.BlacklistService;
import pbl.backend.kchi.modules.users.resources.MessageResource;
import pbl.backend.kchi.services.JwtService;

import java.util.Date;


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
    public AuthController(
            UserServiceInterface userService
    ) {
        this.userService = userService;

    }
    @PostMapping("login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Object result = userService.authenticate(request);

        if(result instanceof LoginResources loginResources) {
            return ResponseEntity.ok(loginResources);
        }

        if(result instanceof ErrorResource errorResource) {
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
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new pbl.backend.kchi.resources.MessageResource("Network Error!"));
        }

    }


}
