package pbl.backend.kchi.modules.messages.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.repositories.ConversationsRepository;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.services.interfaces.ConversationServiceInterface;

import pbl.backend.kchi.services.BaseService;

import java.util.Map;

@Service
public class ConversationService extends BaseService implements ConversationServiceInterface {

    @Autowired
    private ConversationsRepository conversationsRepository;

    @Override
    @Transactional
    public Conversation create(StoreConversationRequest request) {
        try {
            Conversation payload = Conversation.builder()
                    .name(request.getName())
                    .build();
            return conversationsRepository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }


    }

    @Override
    public Page<Conversation> paginate(Map<String, String[]> parameters) {
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page")[0]) : 1;
        int perpage = parameters.containsKey("perpage") ? Integer.parseInt(parameters.get("perpage")[0]) : 20;
        String sortParam = parameters.containsKey("sort") ? parameters.get("sort")[0] : null;
        Sort sort = createSort(sortParam);
        Pageable pageable = PageRequest.of(page - 1, perpage, sort);
        return conversationsRepository.findAll(pageable);
    }

}
