package pbl.backend.kchi.modules.classes.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.annotations.RequirePermission;
import pbl.backend.kchi.enum1.PermissionEnum;


import pbl.backend.kchi.modules.classes.entities.Classes;
import pbl.backend.kchi.modules.classes.mapper.ClassMapper;
import pbl.backend.kchi.modules.classes.repositories.ClassRepository;
import pbl.backend.kchi.modules.classes.requests.AddMembersRequest;
import pbl.backend.kchi.modules.classes.requests.JoinClassRequest;
import pbl.backend.kchi.modules.classes.requests.StoreClassRequest;
import pbl.backend.kchi.modules.classes.requests.UpdateClassRequest;
import pbl.backend.kchi.modules.classes.resources.ClassResource;
import pbl.backend.kchi.modules.classes.services.impl.ClassService;
import pbl.backend.kchi.modules.classes.services.interfaces.ClassServiceInterface;
import pbl.backend.kchi.modules.users.resources.UserResource;
import pbl.backend.kchi.resources.ApiResource;

import java.util.List;


@Tag(name="API Lớp")
@Validated
@RestController
@RequestMapping("api/v1/classes")
public class ClassController extends BaseController<
        Classes,
        ClassResource,
        StoreClassRequest,
        UpdateClassRequest,
        ClassRepository
        > {
    public ClassController(
            ClassServiceInterface service,
            ClassMapper mapper,
            ClassRepository repo
    ){
        super(service, mapper, repo, PermissionEnum.CLASSES_USER);
    }


    @GetMapping("/{id}/members")
    @RequirePermission(action = "show")
    public ResponseEntity<?> getMembers(@PathVariable("id") Long id) {

        List<UserResource> members = ((ClassService) service).getClassMembers(id);

        return ResponseEntity.ok(ApiResource.ok(members, "SUCCESS"));
    }

    @PostMapping("/{id}/members")
    @RequirePermission(action = "update") // Cần quyền cập nhật lớp để thêm thành viên
    public ResponseEntity<?> addMembers(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddMembersRequest request
    ) {
        try {
            // Gọi hàm addMembersByEmail trong ClassService
            ((ClassService) service).addMembersByEmail(id, request.getUserEmails());

            return ResponseEntity.ok(ApiResource.message("Thêm thành viên thành công!", HttpStatus.OK));

        } catch (RuntimeException e) {
            // Bắt các lỗi nghiệp vụ từ Service (ví dụ: Không tìm thấy email, không có quyền)
            return ResponseEntity.badRequest().body(
                    ApiResource.error("ERROR", "Lỗi khi thêm thành viên: " + e.getMessage(), HttpStatus.BAD_REQUEST)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResource.error("INTERNAL_ERROR", "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinClass(@RequestBody JoinClassRequest request) {
        try {
            if (request.getCode() == null || request.getCode().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResource.error("BAD_REQUEST", "Vui lòng nhập mã lớp", HttpStatus.BAD_REQUEST));
            }

            ((ClassService) service).joinClass(request.getCode());

            return ResponseEntity.ok(ApiResource.message("Tham gia lớp học thành công!", HttpStatus.OK));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResource.error("ERROR", e.getMessage(), HttpStatus.BAD_REQUEST)
            );
        }
    }

}