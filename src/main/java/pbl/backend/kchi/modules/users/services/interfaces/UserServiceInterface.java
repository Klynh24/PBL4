package pbl.backend.kchi.modules.users.services.interfaces;
import jakarta.validation.Valid;
import pbl.backend.kchi.modules.users.resources.LoginResources;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
public interface UserServiceInterface {

    Object authenticate(LoginRequest request);


}
