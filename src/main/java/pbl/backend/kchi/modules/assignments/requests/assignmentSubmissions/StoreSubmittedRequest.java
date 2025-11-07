package pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
public class StoreSubmittedRequest {
    private String fileUrl;

    private String score;

    private Long userId;

    private Long assignmentId;

}
