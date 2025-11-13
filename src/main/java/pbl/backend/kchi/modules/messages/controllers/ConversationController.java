package pbl.backend.kchi.modules.messages.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.mappers.ConversationMapper;
import pbl.backend.kchi.modules.messages.repositories.ConversationsRepository;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.UpdateConversationRequest;
import pbl.backend.kchi.modules.messages.resources.ConversationsResource;
import pbl.backend.kchi.modules.messages.services.interfaces.ConversationServiceInterface;

@Tag(name="API CUỘC TRÒ CHUYỆN")
@Validated
@RestController
@RequestMapping("api/v1/conversations")
public class ConversationController extends BaseController<
        Conversation,
        ConversationsResource,
        StoreConversationRequest,
        UpdateConversationRequest,
        ConversationsRepository
        > {
    public ConversationController(
            ConversationServiceInterface service,
            ConversationMapper mapper,
            ConversationsRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.CONVERSATIONS);
    }


}