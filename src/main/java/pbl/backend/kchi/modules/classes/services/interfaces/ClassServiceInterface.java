package pbl.backend.kchi.modules.classes.services.interfaces;

import org.springframework.data.domain.Page;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.users.entities.User;

import java.util.Map;


public interface ClassServiceInterface {
    Classes create(Long userId,StoreClassRequest request);
    Classes update(Long id, UpdateClassRequest request);
    Page<Classes> paginate(Map<String, String[]> parameters );


}
