package pbl.backend.kchi.modules.assignments.requests;

import lombok.Data;

@Data
public class UpdateAssignmentRequest {
    private String fileUrl;

    private String score;

    private Long userId;

    private Long assignmentId;
}
