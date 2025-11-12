package pbl.backend.kchi.modules.messages.services.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.mappers.MessageMapper;
import pbl.backend.kchi.modules.messages.repositories.ConversationsRepository;
import pbl.backend.kchi.modules.messages.repositories.MessagesRepository;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.requests.UpdateMessageRequest;
import pbl.backend.kchi.modules.messages.services.interfaces.MessageServiceInterface;

import pbl.backend.kchi.services.BaseService;

// Import cho Security
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pbl.backend.kchi.modules.users.entities.User; // <-- KIỂM TRA ĐƯỜNG DẪN NÀY

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.util.StringUtils;

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

    @Autowired
    private ConversationsRepository conversationsRepository;

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public MessageService(
            MessageMapper messageMapper
    ){
        this.messageMapper = messageMapper;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"content"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{};
    }

    @Override
    protected MessagesRepository getRepository(){
        return messagesRepository;
    }

    @Override
    protected MessageMapper getMapper(){
        return messageMapper;
    }

    @Override
    public Messages storeFile(MultipartFile file, Long conversationId, String content, HttpServletRequest request) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + uniqueFileName + ". Please try again!", ex);
        }

        Conversation conversation = conversationsRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User sender = (User) authentication.getPrincipal();


        Messages message = new Messages();
        message.setConversation(conversation);
        message.setUser(sender); // Gán người gửi

        message.setText(content != null ? content : "Đã gửi một tệp.");
        message.setFileUrl(targetLocation.toString());
        message.setFileType(file.getContentType());

        return messagesRepository.save(message);
    }
}