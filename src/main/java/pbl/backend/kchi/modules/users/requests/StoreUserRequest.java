package pbl.backend.kchi.modules.users.requests;
import jakarta.validation.constraints.*;

import lombok.*;

@Data
public class StoreUserRequest {
    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Vai trò không được bỏ trống")
    private Long userCatalogueId;

    private String image;

}
