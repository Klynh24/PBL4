package pbl.backend.kchi.modules.users.services.interfaces;

import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;


public interface UserServiceInterface {

    Object authenticate(LoginRequest request);
    User create(StoreUserRequest request);
//    User update(Long id, UpdateRequest request);

}
