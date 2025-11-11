package pbl.backend.kchi.modules.rooms.mapper; // (Gói của bạn có thể khác)

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.rooms.entities.Room;
import pbl.backend.kchi.modules.rooms.requests.StoreRoomRequest;
import pbl.backend.kchi.modules.rooms.requests.UpdateRoomRequest;
import pbl.backend.kchi.modules.rooms.resources.RoomResource;


@Mapper(componentModel = "spring")
public interface RoomMapper extends BaseMapper<
        Room,
        RoomResource,
        StoreRoomRequest,
        UpdateRoomRequest
        > {

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Room toEntity(StoreRoomRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "participants", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateRoomRequest updateRequest, @MappingTarget Room entity);
}