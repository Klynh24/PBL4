package pbl.backend.kchi.modules.messages.services.interfaces;

import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.messages.requests.StoreMessageRequest;
import pbl.backend.kchi.modules.messages.requests.UpdateMessageRequest;

import pbl.backend.kchi.services.interfaces.BaseServiceInterface;


public interface MessageServiceInterface extends BaseServiceInterface<Messages, StoreMessageRequest, UpdateMessageRequest> {

}
