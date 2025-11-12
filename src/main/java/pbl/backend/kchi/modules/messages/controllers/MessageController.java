package pbl.backend.kchi.modules.messages.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.mappers.MessageMapper;
import pbl.backend.kchi.modules.messages.repositories.MessagesRepository;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.requests.UpdateMessageRequest;
import pbl.backend.kchi.modules.messages.resources.MessagesResource;
import pbl.backend.kchi.modules.messages.services.interfaces.MessageServiceInterface;
import pbl.backend.kchi.resources.ApiResource;

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

    private final MessageServiceInterface messageService;

    public MessageController(
            MessageServiceInterface service,
            MessageMapper mapper,
            MessagesRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.MESSAGE);
        this.messageService = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResource<MessagesResource>> uploadFile(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") Long conversationId,
            @RequestParam(name = "content", required = false) String content
    ) {
        Messages message = this.messageService.storeFile(file, conversationId, content, request);

        MessagesResource resource = (MessagesResource) this.mapper.tResource(message);

        return new ResponseEntity<>(
                ApiResource.ok(resource, "File uploaded successfully"),
                HttpStatus.CREATED
        );
    }
}


