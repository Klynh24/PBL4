package pbl.backend.kchi.modules.users.services.interfaces;



import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;


public interface UserCatalogueServiceInterface {
   UserCatalogue create(StoreRequest request);
   UserCatalogue update(Long id, UpdateRequest request);
   Boolean delete(Long id);
   Boolean deleteMultipleEntity(List<Long> ids);

    Page<UserCatalogue> paginate(Map<String, String[]> parameters );
//   List<UserCatalogue> getAll(Map<String, String[]> parameters );

}
