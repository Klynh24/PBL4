package pbl.backend.kchi.modules.classes.resources;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassResource {
    private final Long id;
    private final Long userId;
    private final String name;
    private final String description;
}
