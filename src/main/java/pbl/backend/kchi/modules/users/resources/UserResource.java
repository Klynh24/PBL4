package pbl.backend.kchi.modules.users.resources;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResource {
      private final Long id;
    private final String email;
    private final String name;
    private final String password;
    private final String phone;
    private final String address;
    private final String image;
    private List<String> roles;
}
