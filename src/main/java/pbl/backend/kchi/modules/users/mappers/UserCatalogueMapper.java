package pbl.backend.kchi.modules.users.mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;


@Mapper(componentModel = "spring")
public interface  UserCatalogueMapper extends BaseMapper<UserCatalogue, UserCatalogueResource, StoreRequest, UpdateRequest> {


    @Override
    @BaseMapperAnnotation
    @Mapping(target = "permissions", ignore= true)
    @Mapping(target = "users", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
    UserCatalogue toEntity(StoreRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "permissions", ignore= true)
    @Mapping(target = "users", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateRequest UpdateRequest, @MappingTarget UserCatalogue entity);

}
