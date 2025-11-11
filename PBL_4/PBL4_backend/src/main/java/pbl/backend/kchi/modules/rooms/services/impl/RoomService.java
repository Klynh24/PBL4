package pbl.backend.kchi.modules.rooms.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.rooms.entities.Room;
import pbl.backend.kchi.modules.rooms.mapper.RoomMapper;
import pbl.backend.kchi.modules.rooms.repositories.RoomRepository;
import pbl.backend.kchi.modules.rooms.requests.StoreRoomRequest;
import pbl.backend.kchi.modules.rooms.services.interfaces.RoomServiceInterface;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.mappers.UserMapper;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import pbl.backend.kchi.modules.users.resources.UserResource;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService implements RoomServiceInterface {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private UserMapper userMapper;


    @Override
    @Transactional
    public Room createRoom(StoreRoomRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tạo"));

        Room room = roomMapper.toEntity(request);

        room.getParticipants().add(creator);

        return roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng"));
    }

    @Override
    @Transactional
    public void joinRoom(Long roomId, Long userId) {
        Room room = findRoomById(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        room.getParticipants().add(user);

        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        Room room = findRoomById(roomId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        room.getParticipants().remove(user);

        roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResource> getParticipants(Long roomId) {
        Room room = findRoomById(roomId);

        return room.getParticipants().stream()
                .map(userMapper::tResource) // Dùng phương thức tResource từ BaseMapper
                .collect(Collectors.toList());
    }
}