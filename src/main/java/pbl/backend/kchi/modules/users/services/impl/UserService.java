package pbl.backend.kchi.modules.users.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.mappers.UserMapper;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;
import pbl.backend.kchi.modules.users.services.interfaces.UserServiceInterface;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.services.BaseService;
import pbl.backend.kchi.services.JwtService;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.modules.users.resources.LoginResources;

@Service
public class UserService extends BaseService<
        User,
        UserMapper,
        StoreUserRequest,
        UpdateUserRequest,
        UserRepository
        > implements UserServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.defaultExpiration}")
    private long defaultExpiration;

    private final UserMapper userMapper;


    public UserService(
            UserMapper userMapper
    ){
        this.userMapper = userMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name", "phone", "email"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{"userCatalogues"};
    }

    @Override
    protected UserRepository getRepository(){
        return userRepository;
    }

    @Override
    protected UserMapper getMapper(){
        return userMapper;
    }



    @Override
    public Object authenticate(LoginRequest request){
        try {

            User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác"));
            if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
                throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
            }
            UserResource userResource = UserResource.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .build();


            String token = jwtService.generateToken(user.getId(), user.getEmail(), defaultExpiration);
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());


            return new LoginResources(token, refreshToken, userResource);
        } catch (BadCredentialsException e) {
            logger.error("Lỗi xác thực : {}", e.getMessage());

            return ApiResource.error("AUTH_ERROR", e.getMessage(), HttpStatus.UNAUTHORIZED);

        }
    }

    @Override
    protected void preProcessRequest(StoreUserRequest request){
        if(request.getPassword() != null){
            request.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

}