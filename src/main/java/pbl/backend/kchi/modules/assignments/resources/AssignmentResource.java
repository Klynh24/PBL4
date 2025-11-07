package pbl.backend.kchi.modules.assignments.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentResource {
    private final Long id;
    private final Long classId;
    private final String title;
    private final String description;
    private final LocalDateTime dueDate;
}
