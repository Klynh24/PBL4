package pbl.backend.kchi.modules.messages.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagesResource {
    private final Long id;
    private final Long userId;
    private final Long conversationId;
    private final String text;
}
