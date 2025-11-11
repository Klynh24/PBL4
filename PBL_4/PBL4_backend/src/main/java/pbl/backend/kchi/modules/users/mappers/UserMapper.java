package pbl.backend.kchi.modules.users.mappers;

import org.mapstruct.*;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.requests.StoreUserRequest;
import pbl.backend.kchi.modules.users.requests.UpdateUserRequest;

import pbl.backend.kchi.modules.users.resources.UserResource;

@Mapper(componentModel = "spring")
public interface  UserMapper extends BaseMapper<User, UserResource, StoreUserRequest, UpdateUserRequest> {


    @Override
    @BaseMapperAnnotation
    @Mapping(target = "userCatalogues", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy= NullValuePropertyMappingStrategy.IGNORE)
    User toEntity(StoreUserRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "userCatalogues", ignore= true)
    @Mapping(target = "password", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateUserRequest UpdateUserRequest, @MappingTarget User entity);

}