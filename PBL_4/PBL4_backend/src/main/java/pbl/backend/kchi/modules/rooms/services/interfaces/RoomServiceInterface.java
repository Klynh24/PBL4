package pbl.backend.kchi.modules.rooms.services.interfaces;

import pbl.backend.kchi.modules.rooms.entities.Room;
import pbl.backend.kchi.modules.rooms.requests.StoreRoomRequest;
import pbl.backend.kchi.modules.users.resources.UserResource;

import java.util.List;

public interface RoomServiceInterface {
    Room createRoom(StoreRoomRequest request, Long creatorId);


    Room findRoomById(Long roomId);

    void joinRoom(Long roomId, Long userId);


    void leaveRoom(Long roomId, Long userId);


    List<UserResource> getParticipants(Long roomId);
}