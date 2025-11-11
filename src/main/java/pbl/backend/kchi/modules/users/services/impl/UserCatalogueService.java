package pbl.backend.kchi.modules.users.services.impl;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.mappers.UserCatalogueMapper;
import pbl.backend.kchi.modules.users.repositories.UserCataloguesRespository;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.services.BaseService;

@Service
public class UserCatalogueService extends BaseService<
        UserCatalogue,
        UserCatalogueMapper,
        StoreRequest,
        UpdateRequest,
        UserCataloguesRespository
        > implements  UserCatalogueServiceInterface {

    private final UserCatalogueMapper userCatalogueMapper;

    @Autowired
    private UserCataloguesRespository userCatalogueRepository;

    public UserCatalogueService(
            UserCatalogueMapper userCatalogueMapper
    ){
        this.userCatalogueMapper = userCatalogueMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{"permissions"};
    }

    @Override
    protected UserCataloguesRespository getRepository(){
        return userCatalogueRepository;
    }

    @Override
    protected UserCatalogueMapper getMapper(){
        return userCatalogueMapper;
    }


}