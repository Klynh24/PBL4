package pbl.backend.kchi.modules.messages.controllers;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.messages.entities.Conversations;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.resources.ConversationsResource;
import pbl.backend.kchi.modules.messages.services.interfaces.ConversationServiceInterface;


import pbl.backend.kchi.resources.ApiResource;

import java.util.Map;

@Validated
@RestController
@RequestMapping("api/v1")
public class ConversationController {
    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);
    private final ConversationServiceInterface conversationService;

    public ConversationController(
            ConversationServiceInterface conversationService
    ) {
        this.conversationService = conversationService;

    }

    @PostMapping("/conversations")
    public ResponseEntity<?> store(@Valid @RequestBody StoreConversationRequest request) {
        Conversations conversations = conversationService.create(request);
        ConversationsResource conversationsResource = ConversationsResource.builder()
                .id(conversations.getId())
                .name(conversations.getName())
                .build();
        ApiResource<ConversationsResource> response = ApiResource.ok(conversationsResource, "Thêm mới bản ghi thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);
    }
    @GetMapping("conversations")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<Conversations> conversations = conversationService.paginate(parameters);
        Page<ConversationsResource> conversationsResource = conversations.map(conversation ->
                ConversationsResource.builder()
                        .id(conversation.getId())
                        .name(conversation.getName())
                        .build()
        );

        ApiResource<Page<ConversationsResource>> response = ApiResource.ok(conversationsResource, "SUCCESS");

        logger.info("Method getUserCatalogues Running....");
        return ResponseEntity.ok(response);
    }

}
