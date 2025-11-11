package pbl.backend.kchi.modules.messages.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.mappers.ConversationMapper;
import pbl.backend.kchi.modules.messages.mappers.MessageMapper;
import pbl.backend.kchi.modules.messages.repositories.ConversationsRepository;
import pbl.backend.kchi.modules.messages.repositories.MessagesRepository;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.requests.UpdateMessageRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.UpdateConversationRequest;
import pbl.backend.kchi.modules.messages.resources.ConversationsResource;
import pbl.backend.kchi.modules.messages.resources.MessagesResource;
import pbl.backend.kchi.modules.messages.services.interfaces.ConversationServiceInterface;
import pbl.backend.kchi.modules.messages.services.interfaces.MessageServiceInterface;

import pbl.backend.kchi.resources.ApiResource;

import java.util.Map;

@Tag(name="API TIN NHẮN")
@Validated
@RestController
@RequestMapping("api/v1/messages")
public class MessageController extends BaseController<
        Messages,
        MessagesResource,
        StoreMessageRequest,
        UpdateMessageRequest,
        MessagesRepository
        > {
    public MessageController(
            MessageServiceInterface service,
            MessageMapper mapper,
            MessagesRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.MESSAGE);
    }


}