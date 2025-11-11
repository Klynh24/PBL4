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


    @NotNull(message = "Trạng thái không được để trống")
    @Min(value = 0, message="Giá trị trạng thái phải lớn hơn hoặc bằng 0")
    @Max(value = 2, message= "Giá trị trạng thái phải nhỏ hơn hoặc bằng 2")
    private Integer publish;


    @NotNull(message = "Chưa cấp quyền cho thành viên")
    private List<Long> userCatalogues;

}