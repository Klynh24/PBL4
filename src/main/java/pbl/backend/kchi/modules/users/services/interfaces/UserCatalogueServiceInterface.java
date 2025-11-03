package pbl.backend.kchi.modules.users.services.interfaces;



import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;

public interface UserCatalogueServiceInterface {
   UserCatalogue create(StoreRequest request);
   UserCatalogue update(Long id, UpdateRequest request);

}
