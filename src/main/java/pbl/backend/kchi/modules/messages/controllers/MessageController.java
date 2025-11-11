package pbl.backend.kchi.modules.messages.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.resources.MessagesResource;
import pbl.backend.kchi.modules.messages.services.interfaces.MessageServiceInterface;

import pbl.backend.kchi.resources.ApiResource;

import java.util.Map;

@Validated
@RestController
@RequestMapping("api/v1")
public class MessageController {
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageServiceInterface messageService;

    public MessageController(
            MessageServiceInterface messageService
    ) {
        this.messageService = messageService;

    }

    @PostMapping("/messages/{id}")
    public ResponseEntity<?> store(
            @PathVariable Long id,
            @Valid @RequestBody StoreMessageRequest request) {
        Messages messages = messageService.create(id, request);

        MessagesResource messageResource = MessagesResource.builder()
                .id(messages.getId())
                .conversationId(messages.getConversationId())
                .userId(id)
                .text(messages.getText())
                .build();
        ApiResource<MessagesResource> response = ApiResource.ok(messageResource, "Thêm mới bản ghi thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);

    }

    @GetMapping("messages")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<Messages> messages = messageService.paginate(parameters);
        Page<MessagesResource> messagesResource = messages.map(messages1 ->
                MessagesResource.builder()
                        .id(messages1.getId())
                        .conversationId(messages1.getConversationId())
                        .userId(messages1.getUserId())
                        .text(messages1.getText())
                        .build()
        );

        ApiResource<Page<MessagesResource>> response = ApiResource.ok(messagesResource, "SUCCESS");

        logger.info("Method getUserCatalogues Running....");
        return ResponseEntity.ok(response);
    }


}
