package pbl.backend.kchi.modules.classes.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.resources.ClassResource;
import pbl.backend.kchi.modules.users.mappers.UserMapper;


@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ClassMapper extends BaseMapper<Classes, ClassResource, StoreClassRequest, UpdateClassRequest> {

    @Override
    @BaseMapperAnnotation

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "members", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Classes toEntity(StoreClassRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "members", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateClassRequest updateRequest, @MappingTarget Classes entity);
}