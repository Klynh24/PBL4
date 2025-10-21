package pbl.backend.kchi.modules.users.controllers;
//đăng nhập

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.resources.LoginResources;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.resources.ErrorResource;

@Validated
@RestController
@RequestMapping("v1/auth")
public class AuthController {

    private final UserServiceInterface userService;
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

}
