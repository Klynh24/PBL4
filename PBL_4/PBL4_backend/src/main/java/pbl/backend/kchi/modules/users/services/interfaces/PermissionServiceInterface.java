package pbl.backend.kchi.modules.users.services.interfaces;

import pbl.backend.kchi.modules.users.entities.Permission;
import pbl.backend.kchi.modules.users.requests.Permission.StoreRequest;
import pbl.backend.kchi.modules.users.requests.Permission.UpdateRequest;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;

public interface PermissionServiceInterface extends BaseServiceInterface<Permission, StoreRequest, UpdateRequest> {

}