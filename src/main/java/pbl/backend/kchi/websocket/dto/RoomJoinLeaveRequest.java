package pbl.backend.kchi.websocket.dto;

import jakarta.validation.constraints.NotNull;

public class RoomJoinLeaveRequest {

    @NotNull
    private Long roomId;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
