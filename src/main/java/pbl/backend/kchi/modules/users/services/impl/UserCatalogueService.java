package pbl.backend.kchi.modules.users.services.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.LoginRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.services.BaseService;
import pbl.backend.kchi.modules.users.repositories.UserCataloguesRespository;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
public class UserCatalogueService extends BaseService implements UserCatalogueServiceInterface {
    @Autowired
    private UserCataloguesRespository userCataloguesRespository;

    @Override
    @Transactional
    public UserCatalogue create(StoreRequest request) {
        try {
            UserCatalogue payload = UserCatalogue.builder()
                    .name(request.getName())
                    .publish(request.getPublish())
                    .build();
            return userCataloguesRespository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }

    }

    @Override
    @Transactional
    public UserCatalogue update(Long id, UpdateRequest request) {
       UserCatalogue userCatalogue = userCataloguesRespository.findById(id)
               .orElseThrow(() -> new EntityNotFoundException("Nhóm thành viên không tồn tại"));

       UserCatalogue payload = userCatalogue.toBuilder()
               .name(request.getName())
               .publish(request.getPublish())
               .build();
       return userCataloguesRespository.save(payload);
    }
}
