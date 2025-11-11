package pbl.backend.kchi.modules.messages.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.mappers.ConversationMapper;
import pbl.backend.kchi.modules.messages.repositories.ConversationsRepository;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.UpdateConversationRequest;
import pbl.backend.kchi.modules.messages.services.interfaces.ConversationServiceInterface;
import pbl.backend.kchi.services.BaseService;


@Service
public class ConversationService extends BaseService<
        Conversation,
        ConversationMapper,
        StoreConversationRequest,
        UpdateConversationRequest,
        ConversationsRepository
        > implements ConversationServiceInterface {

    private final ConversationMapper conversationMapper;

    @Autowired
    private ConversationsRepository conversationsRepository;

    public ConversationService(
            ConversationMapper conversationMapper
    ){
        this.conversationMapper = conversationMapper;
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
    protected ConversationsRepository getRepository(){
        return conversationsRepository;
    }

    @Override
    protected ConversationMapper getMapper(){
        return conversationMapper;
    }


}