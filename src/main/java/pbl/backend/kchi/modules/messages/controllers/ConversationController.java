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
import pbl.backend.kchi.modules.messages.mappers.ConversationMapper;
import pbl.backend.kchi.modules.messages.repositories.ConversationsRepository;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.UpdateConversationRequest;
import pbl.backend.kchi.modules.messages.resources.ConversationsResource;
import pbl.backend.kchi.modules.messages.services.interfaces.ConversationServiceInterface;


import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.mappers.UserCatalogueMapper;
import pbl.backend.kchi.modules.users.repositories.UserCataloguesRespository;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.UpdateRequest;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

import java.util.Map;

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