package pbl.backend.kchi.modules.assignments.requests.assignmentSubmissions;

import lombok.Data;

@Data

public class UpdateSubmittedRequest {
    private String fileUrl;

    private String score;

    private Long userId;

    private Long assignmentId;
}
