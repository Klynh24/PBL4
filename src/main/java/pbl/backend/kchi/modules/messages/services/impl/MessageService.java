package pbl.backend.kchi.modules.messages.services.impl;

import org.aspectj.bridge.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.messages.entities.Conversations;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.repositories.MessagesRepositories;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.services.interfaces.MessageServiceInterface;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.services.BaseService;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

@Service
public class MessageService extends BaseService implements MessageServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessagesRepositories messagesRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Messages create(Long userId, StoreMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Bạn phải đăng nhập để nhắn tin"));
        Long userid = user.getId();
        try {

            Messages payload = Messages.builder()
                    .text(request.getText())
                    .userId(userid)
                    .conversationId(request.getConversationId())
                    .build();
            return messagesRepository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }
    }

    @Override
    public Page<Messages> paginate(Map<String, String[]> parameters) {
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page")[0]) : 1;
        int perpage = parameters.containsKey("perpage") ? Integer.parseInt(parameters.get("perpage")[0]) : 20;
        String sortParam = parameters.containsKey("sort") ? parameters.get("sort")[0] : null;
        Sort sort = createSort(sortParam);
        Pageable pageable = PageRequest.of(page - 1, perpage, sort);
        return messagesRepository.findAll(pageable);
    }
}
