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


    @PostMapping
    public ResponseEntity<ApiResource<RoomResource>> createRoom(
            @Valid @RequestBody StoreRoomRequest request,
            HttpServletRequest httpServletRequest
    ) {
        Long creatorId = (Long) httpServletRequest.getAttribute("userId");
        Room newRoom = roomService.createRoom(request, creatorId);


        RoomResource roomResource = roomMapper.tResource(newRoom);


        ApiResource<RoomResource> response = ApiResource.ok(roomResource, "Tạo phòng thành công");
        return new ResponseEntity<>(response, response.getStatus());
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
            @PathVariable Long roomId,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = (Long) httpServletRequest.getAttribute("userId");
        roomService.joinRoom(roomId, userId);

        ApiResource<String> response = ApiResource.message("Gia nhập phòng thành công", HttpStatus.OK);
        return new ResponseEntity<>(response, response.getStatus());
    }


    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResource<String>> leaveRoom(
            @PathVariable Long roomId,
            HttpServletRequest httpServletRequest
    ) {
        Long userId = (Long) httpServletRequest.getAttribute("userId");
        roomService.leaveRoom(roomId, userId);

        ApiResource<String> response = ApiResource.message("Rời phòng thành công", HttpStatus.OK);
        return new ResponseEntity<>(response, response.getStatus());
    }


    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ApiResource<List<UserResource>>> getParticipants(
            @PathVariable Long roomId
    ) {
        List<UserResource> participants = roomService.getParticipants(roomId);

        ApiResource<List<UserResource>> response = ApiResource.ok(participants, "Lấy danh sách thành viên thành công");
        return new ResponseEntity<>(response, response.getStatus());
    }
}