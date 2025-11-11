package pbl.backend.kchi.modules.users.services.interfaces;



import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import org.springframework.data.domain.Page;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;

import java.util.List;
import java.util.Map;



public interface UserCatalogueServiceInterface extends BaseServiceInterface<UserCatalogue, StoreRequest, UpdateRequest> {

}
