package pbl.backend.kchi.modules.messages.mappers;

import org.mapstruct.*;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.UpdateConversationRequest;
import pbl.backend.kchi.modules.messages.resources.ConversationsResource;


@Mapper(componentModel = "spring")
public interface  ConversationMapper extends BaseMapper<Conversation, ConversationsResource, StoreConversationRequest, UpdateConversationRequest> {


    @Override
    @BaseMapperAnnotation
    @Mapping(target = "members", ignore= true)
    @Mapping(target = "messages", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy= NullValuePropertyMappingStrategy.IGNORE)
    Conversation toEntity(StoreConversationRequest createRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "members", ignore= true)
    @Mapping(target = "messages", ignore= true)
    @BeanMapping(nullValuePropertyMappingStrategy=NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateConversationRequest UpdateRequest, @MappingTarget Conversation entity);

}