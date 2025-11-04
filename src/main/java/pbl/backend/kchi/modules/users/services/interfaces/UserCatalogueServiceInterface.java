package pbl.backend.kchi.modules.users.services.interfaces;



import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import org.springframework.data.domain.Page;

import java.util.Map;


public interface UserCatalogueServiceInterface {
   UserCatalogue create(StoreRequest request);
   UserCatalogue update(Long id, UpdateRequest request);
   Page<UserCatalogue> paginate(Map<String, String[]> parameters );

}
