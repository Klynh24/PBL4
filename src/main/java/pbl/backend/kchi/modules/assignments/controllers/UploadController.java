package pbl.backend.kchi.modules.assignments.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pbl.backend.kchi.annotations.RequirePermission; // ⭐ Cần Import này
import pbl.backend.kchi.resources.ApiResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private final String UPLOAD_DIR_RELATIVE = "./uploads/";

    @PostMapping
    // ⭐ ÁP DỤNG QUYỀN: Sử dụng chuỗi quyền đầy đủ (upload:create)
    @RequirePermission(action = "upload:create")
    public ResponseEntity<ApiResource<String>> uploadFile(
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResource.error("FILE_EMPTY", "Tập tin không được để trống", null));
        }

        try {
            // 1. Lấy đường dẫn và xử lý path
            Path uploadPath = Paths.get(UPLOAD_DIR_RELATIVE);

            // 2. TẠO THƯ MỤC NẾU CHƯA TỒN TẠI
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created missing upload directory: {}", uploadPath.toAbsolutePath());
            }

            // 3. Xử lý tên file và lưu
            String originalFileName = file.getOriginalFilename();
            String uniqueFileName = System.currentTimeMillis() + "_" + originalFileName;
            Path filePath = uploadPath.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Trả về URL
            String fileUrl = "/uploads/" + uniqueFileName;
            logger.info("File saved successfully at URL: {}", fileUrl);

            return ResponseEntity.ok(ApiResource.ok(fileUrl, "Upload thành công"));

        } catch (IOException e) {
            logger.error("Lỗi I/O nghiêm trọng khi lưu trữ file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ApiResource.error("SERVER_ERROR", "Lỗi khi lưu trữ file (Kiểm tra quyền ghi/đường dẫn): " + e.getMessage(), null)
            );
        }
    }
}