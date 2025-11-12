package pbl.backend.kchi.modules.users.requests;

import jakarta.validation.constraints.*;

import lombok.*;

import java.util.List;


@Data
public class UpdateUserRequest {
    @NotBlank(message = "Tên thành viên không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Điện thoại không được để trống")
    private String phone;


    private String address;
    private String image;




    @NotNull(message = "Chưa cấp quyền cho thành viên")
    private List<Long> userCatalogues;

}
