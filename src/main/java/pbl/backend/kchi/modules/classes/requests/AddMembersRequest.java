package pbl.backend.kchi.modules.classes.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddMembersRequest {
    @NotEmpty(message = "Danh sách email không được để trống")
    private List<String> userEmails;

}