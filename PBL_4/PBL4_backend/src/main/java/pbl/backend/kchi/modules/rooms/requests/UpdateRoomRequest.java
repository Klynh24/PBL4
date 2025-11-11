package pbl.backend.kchi.modules.rooms.requests;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateRoomRequest {
    private String name;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}