package pbl.backend.kchi.modules.users.mappers;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.users.entities.Permission;
import pbl.backend.kchi.modules.users.requests.Permission.StoreRequest;
import pbl.backend.kchi.modules.users.requests.Permission.UpdateRequest;
import pbl.backend.kchi.modules.users.resources.PermissionResource;


@Mapper(componentModel = "spring")
public interface  PermissionMapper extends BaseMapper<Permission, PermissionResource, StoreRequest, UpdateRequest> {

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "userCatalogues", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
    Permission toEntity(StoreRequest createRequest);


    @Override
    @Mapping(target = "userCatalogues", ignore = true)
    @BaseMapperAnnotation
    void updateEntityFromRequest(UpdateRequest updateRequest, @MappingTarget Permission entity);

}