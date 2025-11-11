package pbl.backend.kchi.modules.messages.mappers;

import org.mapstruct.*;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.requests.UpdateMessageRequest;
import pbl.backend.kchi.modules.messages.resources.MessagesResource;

@Mapper(componentModel = "spring")
public interface MessageMapper extends BaseMapper<Messages, MessagesResource, StoreMessageRequest, UpdateMessageRequest> {


    @Override
    @BaseMapperAnnotation
    @Mapping(target = "conversation", ignore= true)
    @Mapping(target = "user", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy= NullValuePropertyMappingStrategy.IGNORE)
    Messages toEntity(StoreMessageRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "conversation", ignore= true)
    @Mapping(target = "user", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateMessageRequest UpdateRequest, @MappingTarget Messages entity);

}
