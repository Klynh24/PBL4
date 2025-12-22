package pbl.backend.kchi.modules.rooms.services.impl;
import java.time.LocalDateTime;
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
import java.util.Optional;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;

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

    @Autowired // BẮT BUỘC PHẢI THÊM CÁI NÀY
    private ClassRepository classRepository;


  

    @Override
    @Transactional(readOnly = true)
    public Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phòng"));
    }

    @Override
@Transactional
public Room createRoom(StoreRoomRequest request, Long creatorId) {
    // 1. Kiểm tra phòng ACTIVE cũ
    return roomRepository.findByClassesIdAndStatus(request.getClassId(), "ACTIVE")
        .orElseGet(() -> {
            Room room = roomMapper.toEntity(request);
            
            // Tìm và gán Entity Class để tránh lỗi NULL class_id
            pbl.backend.kchi.modules.classes.entities.Classes classes = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học"));
            room.setClasses(classes); 

            User creator = userRepository.findById(creatorId).orElseThrow();
            room.setStatus("ACTIVE");
            room.getParticipants().add(creator);
            
            return roomRepository.save(room);
        });
}

@Transactional
public void joinRoom(Long roomId, Long userId) {
    // Lấy đúng roomId đang tồn tại
    Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new EntityNotFoundException("Phòng không tồn tại hoặc đã kết thúc"));
    
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Người dùng không tồn tại"));

    // Thêm sinh viên vào Set participants (Set tự động tránh trùng lặp)
    room.getParticipants().add(user);

    // Lưu lại để cập nhật bảng trung gian room_participants
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