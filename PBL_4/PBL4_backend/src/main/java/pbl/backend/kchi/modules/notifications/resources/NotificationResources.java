package pbl.backend.kchi.modules.notifications.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResources {
    private final Long id;
    private final Long userId;
    private final String message;
    private final String type;
    private final Boolean readStatus;
}