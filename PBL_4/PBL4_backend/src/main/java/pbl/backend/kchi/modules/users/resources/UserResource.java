package pbl.backend.kchi.modules.users.resources;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResource {
    private final Long id;
    private final String email;
    private final String name;
    private final String password;
    private final String phone;
    private final String address;
    private final String image;
    private final Long userCatalogueId;

}