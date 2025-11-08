package pbl.backend.kchi.modules.messages.services.interfaces;

import org.springframework.data.domain.Page;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;

import java.util.Map;

public interface MessageServiceInterface {
    Messages create(Long userId, StoreMessageRequest request);
    Page<Messages> paginate(Map<String, String[]> parameters );

}
