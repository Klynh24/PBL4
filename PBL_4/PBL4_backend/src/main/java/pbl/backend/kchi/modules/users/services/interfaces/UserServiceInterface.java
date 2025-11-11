package pbl.backend.kchi.modules.users.services.interfaces;

import pbl.backend.kchi.modules.users.entities.User;

import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;






public interface UserServiceInterface extends BaseServiceInterface<User, StoreUserRequest, UpdateUserRequest> {

    Object authenticate(LoginRequest request);

}