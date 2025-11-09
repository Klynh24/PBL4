package pbl.backend.kchi.modules.notifications.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;
import pbl.backend.kchi.modules.notifications.resources.NotificationResources;
import pbl.backend.kchi.modules.notifications.services.interfaces.NotificationServiceInterface;
import pbl.backend.kchi.modules.users.controllers.UserCatalogueController;

import pbl.backend.kchi.resources.ApiResource;

import javax.persistence.EntityNotFoundException;
import java.util.Map;


@Validated
@RestController
@RequestMapping("api/v1")
public class NotificationsController {
    private static final Logger logger = LoggerFactory.getLogger(UserCatalogueController.class);
    private final NotificationServiceInterface notificationService;

    public NotificationsController(
            NotificationServiceInterface notificationService
    ) {
        this.notificationService = notificationService ;

    }

    @PostMapping("/notifications")
    public ResponseEntity<?> store(@Valid @RequestBody StoreNotificationRequest request) {
        Notifications notifications = notificationService.create(request);
        NotificationResources notificationResource = NotificationResources.builder()
                .id(notifications.getId())
                .type(notifications.getType())
                .message(notifications.getMessage())
                .userId(notifications.getUserId())
                .readStatus(notifications.getReadStatus())
                .build();
        ApiResource<NotificationResources> response = ApiResource.ok(notificationResource, "Thêm mới bản ghi thành công");
        logger.info("Method Store Running....");
        return ResponseEntity.ok(response);
    }

    @GetMapping("notifications")
    public ResponseEntity<?> index(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        Page<Notifications> notifications = notificationService.paginate(parameters);
        Page<NotificationResources> notificationResource = notifications.map(notification ->
                NotificationResources.builder()
                        .id(notification.getId())
                        .userId(notification.getUserId())
                        .readStatus(notification.getReadStatus())
                        .message(notification.getMessage())
                        .type(notification.getType())
                        .build()
        );

        ApiResource<Page<NotificationResources>> response = ApiResource.ok(notificationResource, "SUCCESS");

        logger.info("Method getUserCatalogues Running....");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/notifications/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNotificationRequest request
    ) {
        logger.info("Method Store Running....");
        try {
            Notifications notification = notificationService.update(id, request);
            NotificationResources notificationResource = NotificationResources.builder()
                    .id(notification.getId())
                    .userId(notification.getUserId())
                    .readStatus(notification.getReadStatus())
                    .message(notification.getMessage())
                    .type(notification.getType())
                    .build();
            ApiResource<NotificationResources> response = ApiResource.ok(notificationResource, "Cập nhật bản ghi thành công");

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResource.error("NOT_FOUND",e.getMessage(),HttpStatus.BAD_REQUEST)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResource.error("INTERNAL_SERVER_ERROR","Có lỗi xảy ra trong quá trình cập nhật",
                            HttpStatus.INTERNAL_SERVER_ERROR)

            );
        }
    }
}
