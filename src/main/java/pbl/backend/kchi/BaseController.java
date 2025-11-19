package pbl.backend.kchi;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pbl.backend.kchi.annotations.RequirePermission;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pbl.backend.kchi.enum1.PermissionEnum;
import pbl.backend.kchi.mapper.BaseMapper;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;

@SecurityRequirement(name="Bearer Authentication")
public abstract class BaseController <
        E,
        R,
        C,
        U,
        Rp extends JpaRepository<E, Long> & JpaSpecificationExecutor<E>
        > {

    protected final BaseServiceInterface<E, C, U> service;
    protected final BaseMapper<E, R, C, U> mapper;
    protected final Rp repo;
    public final PermissionEnum module;


    public BaseController(BaseServiceInterface<E, C, U> service, BaseMapper<E, R, C, U> mapper, Rp repo, PermissionEnum module){
        this.service = service;
        this.mapper = mapper;
        this.repo = repo;
        this.module = module;
    }

    public PermissionEnum getModule() {
        return module;
    }

    @Operation(
            summary="Danh sách bản ghi kết hợp với tìm kiếm",
            description = "Trả về danh sách bản ghi và kết hợp với lọc tìm kiếm"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode="200",
                    description="Success",
                    content=@Content(schema = @Schema(implementation = ApiResource.class))
            ),
            @ApiResponse(
                    responseCode="403",
                    description="Không có quyền truy cập",
                    content=@Content(schema = @Schema(implementation = ApiResource.class))
            ),
            @ApiResponse(
                    responseCode="500",
                    description="Có lỗi xảy ra trong quá trình xử lý",
                    content=@Content(schema = @Schema(implementation = ApiResource.class))
            )
    })
    @GetMapping("/list")
    // @RequirePermission(action = "list", viewAll="view_all")
    public ResponseEntity<?> list(HttpServletRequest request) {
        try {
            Map<String, String[]> parameters = request.getParameterMap();
            List<E> entities = service.getAll(parameters, request);
            List<R> resource = mapper.toList(entities);
            ApiResource<List<R>> response = ApiResource.ok(resource, "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String message = "Có lỗi xảy ra trong quá trình xử lý " + e.getMessage();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @GetMapping
    // @RequirePermission(action = "pagination", viewAll="view_all")
    public ResponseEntity<?> pagination(HttpServletRequest request){
        Map<String, String[]> parameters = request.getParameterMap();
        Page<E> entities = service.paginate(parameters, request);
        Page<R> resource = mapper.toResourcePage(entities);
        ApiResource<Page<R>> response = ApiResource.ok(resource, "SUCCESS");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @RequirePermission(action = "store") // Lưu ý: Kiểm tra xem DB bạn đặt là 'classes:store' hay 'classes:create' nhé!
    public ResponseEntity<?> store(@Valid @RequestBody C request){
        try {
            E entity = service.create(request);
            R resource = mapper.tResource(entity);
            ApiResource<R> response = ApiResource.ok(resource, "Thêm mới bản ghi thành công");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra console để debug dễ hơn
            String message = "Có lỗi xảy ra: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @PutMapping("/{id}")
    @RequirePermission(action = "update")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id, // <--- Đã có ("id") -> OK
            @Valid @RequestBody U request
    ){
        try {
            E entity = service.update(id, request);
            R resource = mapper.tResource(entity);
            ApiResource<R> response = ApiResource.ok(resource, "Cập nhật bản ghi thành công");
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND)
            );
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", "Có lỗi xảy ra: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    // --- SỬA LỖI CHÍNH Ở ĐÂY ---
    @GetMapping("/{id}")
    @RequirePermission(action = "show")
    public ResponseEntity<?> show(@PathVariable("id") Long id) { // <--- THÊM ("id") VÀO ĐÂY
        E entity = repo.findById(id).orElseThrow(() ->new RuntimeException("Bản ghi không tồn tại"));
        R resource = mapper.tResource(entity);
        ApiResource<R> response = ApiResource.ok(resource, "SUCCESS");
        return ResponseEntity.ok(response);
    }

    // --- SỬA LỖI CẢ Ở ĐÂY NỮA ---
    @DeleteMapping("/{id}")
    @RequirePermission(action = "delete")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) { // <--- THÊM ("id") VÀO ĐÂY
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResource.message("Xóa bản ghi thành công", HttpStatus.OK));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND)
            );
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", "Có lỗi xảy ra trong quá trình xử lý", HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }

    @DeleteMapping
    @RequirePermission(action = "deleteMany")
    public ResponseEntity<?> deleteMany(@RequestBody List<Long> Ids){
        try {
            service.deleteMultipleEntity(Ids);
            return ResponseEntity.ok(ApiResource.message("Xóa bản ghi thành công", HttpStatus.OK));

        } catch (RuntimeException e) {
            String message = e.getMessage();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND", message, HttpStatus.NOT_FOUND)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR", "Có lỗi xảy ra trong quá trình xử lý", HttpStatus.INTERNAL_SERVER_ERROR)
            );
        }
    }
}