package pbl.backend.kchi.modules.users.services.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.users.entities.Permission;
import pbl.backend.kchi.modules.users.mappers.PermissionMapper;
import pbl.backend.kchi.modules.users.repositories.PermissionRepository;
import pbl.backend.kchi.modules.users.requests.Permission.StoreRequest;
import pbl.backend.kchi.modules.users.requests.Permission.UpdateRequest;
import pbl.backend.kchi.modules.users.services.interfaces.PermissionServiceInterface;
import pbl.backend.kchi.services.BaseService;

@Service
public class PermissionService extends BaseService<
        Permission,
        PermissionMapper,
        StoreRequest,
        UpdateRequest,
        PermissionRepository
        > implements PermissionServiceInterface {

    private final PermissionMapper PermissionMapper;

    @Autowired
    private PermissionRepository PermissionRepository;

    public PermissionService(
            PermissionMapper PermissionMapper
    ){
        this.PermissionMapper = PermissionMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected PermissionRepository getRepository(){
        return PermissionRepository;
    }

    @Override
    protected PermissionMapper getMapper(){
        return PermissionMapper;
    }

    public boolean hasPermission(String requiredPermission, PermissionEnum module, String action){
        String permission = module.getPrefix() + ":" + action;
        return requiredPermission.equals(permission);
    }

}
