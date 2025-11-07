package pbl.backend.kchi.modules.assignments.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;



@Data
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmittedResource {
    private final Long id;
    private final Long assignmentId;
    private final Long userId;
    private final String fileUrl;
    private final String score;
}
