package pbl.backend.kchi.modules.users.services.interfaces;

import pbl.backend.kchi.modules.users.requests.LoginRequest;
public interface UserServiceInterface {

    Object authenticate(LoginRequest request);


}
