package pbl.backend.kchi.modules.rooms.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.rooms.entities.Room;
import pbl.backend.kchi.modules.rooms.mapper.RoomMapper;
import pbl.backend.kchi.modules.rooms.requests.StoreRoomRequest;
import pbl.backend.kchi.modules.rooms.resources.RoomResource;
import pbl.backend.kchi.modules.rooms.services.interfaces.RoomServiceInterface;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.modules.rooms.repositories.RoomRepository;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import java.util.Optional;
import java.util.List;

@Tag(name="API ROOMS")
@Validated
@RestController
@RequestMapping("api/v1/rooms")
public class RoomController {

    @Autowired
    private RoomServiceInterface roomService;

    @Autowired
    private RoomMapper roomMapper;

     @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ClassRepository classRepository;


   @PostMapping
public ResponseEntity<ApiResource<RoomResource>> createRoom(
        @Valid @RequestBody StoreRoomRequest request, // Thêm @RequestBody
        HttpServletRequest httpServletRequest
) {
    // Lấy userId từ request giống như bạn làm ở hàm joinRoom
    Long userId = (Long) httpServletRequest.getAttribute("userId");
    
    // Gọi Service để xử lý (Logic setClasses phải nằm ở Service)
    Room room = roomService.createRoom(request, userId);
    
    RoomResource roomResource = roomMapper.tResource(room);
    ApiResource<RoomResource> response = ApiResource.ok(roomResource, "Tạo phòng thành công");
    
    return new ResponseEntity<>(response, HttpStatus.OK);
}
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResource<RoomResource>> getRoomInfo(
            @PathVariable Long roomId
    ) {
        Room room = roomService.findRoomById(roomId);
        RoomResource roomResource = roomMapper.tResource(room);

        ApiResource<RoomResource> response = ApiResource.ok(roomResource, "Lấy thông tin phòng thành công");
        return new ResponseEntity<>(response, response.getStatus());
    }


    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResource<String>> joinRoom(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = (Long) httpServletRequest.getAttribute("userId");
        roomService.joinRoom(roomId, userId);

        ApiResource<String> response = ApiResource.message("Gia nhập phòng thành công", HttpStatus.OK);
        return new ResponseEntity<>(response, response.getStatus());
    }


    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResource<String>> leaveRoom(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = (Long) httpServletRequest.getAttribute("userId");
        roomService.leaveRoom(roomId, userId);

        ApiResource<String> response = ApiResource.message("Rời phòng thành công", HttpStatus.OK);
        return new ResponseEntity<>(response, response.getStatus());
    }
@GetMapping("/active/{classId}")
public ResponseEntity<?> getActiveRoom(@PathVariable("classId") Long classId) {
    return roomRepository.findByClassesIdAndStatus(classId, "ACTIVE")
        .map(room -> {
            // Sử dụng Map để trả về cấu trúc { "data": { "id": ... } }
            java.util.Map<String, Object> data = java.util.Map.of("id", room.getId());
            return ResponseEntity.ok(java.util.Map.of("data", data));
        })
        // Sử dụng orElseGet để đảm bảo cùng kiểu trả về ResponseEntity<?>
        .orElseGet(() -> ResponseEntity.noContent().build());
}

    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ApiResource<List<UserResource>>> getParticipants(
            @PathVariable("roomId") Long roomId
    ) {
        List<UserResource> participants = roomService.getParticipants(roomId);

        ApiResource<List<UserResource>> response = ApiResource.ok(participants, "Lấy danh sách thành viên thành công");
        return new ResponseEntity<>(response, response.getStatus());
    }
}