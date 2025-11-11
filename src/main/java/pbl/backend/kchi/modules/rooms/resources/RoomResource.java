package pbl.backend.kchi.modules.rooms.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomResource {
    private Long id;
    private String name;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
