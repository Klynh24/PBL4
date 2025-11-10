package pbl.backend.kchi.modules.users.services.interfaces;

import org.springframework.data.domain.Page;
import pbl.backend.kchi.modules.users.entities.User;

import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;

import java.util.Map;


public interface UserServiceInterface {

    Object authenticate(LoginRequest request);
    User create(StoreUserRequest request);
    User update(Long id, UpdateUserRequest request);
    Page<User> paginate(Map<String, String[]> parameters );
    Boolean delete(Long id);


}
