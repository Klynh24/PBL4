package pbl.backend.kchi.modules.messages.services.interfaces;


import pbl.backend.kchi.modules.messages.entities.Conversation;
import pbl.backend.kchi.modules.messages.requests.conversations.StoreConversationRequest;
import pbl.backend.kchi.modules.messages.requests.conversations.UpdateConversationRequest;

import pbl.backend.kchi.services.interfaces.BaseServiceInterface;




public interface ConversationServiceInterface extends BaseServiceInterface<Conversation, StoreConversationRequest, UpdateConversationRequest> {

}
