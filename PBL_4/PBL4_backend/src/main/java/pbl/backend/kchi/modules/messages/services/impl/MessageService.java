package pbl.backend.kchi.modules.messages.services.impl;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.mappers.MessageMapper;
import pbl.backend.kchi.modules.messages.repositories.MessagesRepository;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.requests.UpdateMessageRequest;
import pbl.backend.kchi.modules.messages.services.interfaces.MessageServiceInterface;


import pbl.backend.kchi.services.BaseService;



@Service
public class MessageService extends BaseService<
        Messages,
        MessageMapper,
        StoreMessageRequest,
        UpdateMessageRequest,
        MessagesRepository
        > implements MessageServiceInterface {

    private final MessageMapper messageMapper;

    @Autowired
    private MessagesRepository messagesRepository;

    public MessageService(
            MessageMapper messageMapper
    ){
        this.messageMapper = messageMapper;
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
    protected MessagesRepository getRepository(){
        return messagesRepository;
    }

    @Override
    protected MessageMapper getMapper(){
        return messageMapper;
    }


}