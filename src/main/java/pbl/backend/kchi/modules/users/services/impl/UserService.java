package pbl.backend.kchi.modules.users.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;

import pbl.backend.kchi.services.BaseService;
import pbl.backend.kchi.modules.users.resources.LoginResources;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.services.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;

import javax.persistence.EntityNotFoundException;

@Service
public class UserService extends BaseService implements UserServiceInterface  {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.defaultExpiration}")
    private long defaultExpiration;

    @Override
    @Transactional
    public User create(StoreUserRequest request) {
        try {

            User payload = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .image(request.getImage())
                    .userCatalogueid(request.getUserCatalogueId())
                    .build();
            return userRepository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public User update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nhóm thành viên không tồn tại"));
            User payload = user.toBuilder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .image(request.getImage())
                    .userCatalogueid(request.getUserCatalogueId())
                    .build();
            return userRepository.save(payload);

    }


    @Override
    public Object authenticate(LoginRequest request) {
        try {

            User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác"));

            if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
            }


            UserResource userResource = new UserResource(user.getId(), user.getEmail(), user.getName(), user.getPhone(), user.getPhone(), user.getAddress(), user.getImage(), user.getUserCatalogueid());
            String token = jwtService.generateToken(user.getId(), user.getEmail(), defaultExpiration);

            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

            return new LoginResources(token,refreshToken, userResource);



        } catch (BadCredentialsException e) {

            logger.error("Lỗi xác thực {}" , e.getMessage());

            return ApiResource.error("AUTH_ERROR",e.getMessage(), HttpStatus.UNAUTHORIZED);

        }
    }
}