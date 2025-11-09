package pbl.backend.kchi.modules.notifications.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.resources.NotificationResources;
import pbl.backend.kchi.modules.notifications.services.interfaces.NotificationServiceInterface;
import pbl.backend.kchi.modules.users.controllers.UserCatalogueController;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;
import pbl.backend.kchi.modules.users.requests.UserCatalogue.StoreRequest;
import pbl.backend.kchi.modules.users.resources.UserCatalogueResource;
import pbl.backend.kchi.modules.users.services.interfaces.UserCatalogueServiceInterface;
import pbl.backend.kchi.resources.ApiResource;


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
}
