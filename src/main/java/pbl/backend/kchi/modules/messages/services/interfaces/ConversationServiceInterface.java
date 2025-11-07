package pbl.backend.kchi.modules.messages.services.interfaces;


import org.springframework.data.domain.Page;
import pbl.backend.kchi.modules.messages.entities.Conversations;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;

import java.util.Map;


public interface ConversationServiceInterface {
    Conversations create(StoreConversationRequest request);
    Page<Conversations> paginate(Map<String, String[]> parameters );

}
