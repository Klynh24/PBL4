package pbl.backend.kchi.modules.notifications.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pbl.backend.kchi.BaseController;
import pbl.backend.kchi.enum1.PermissionEnum;

import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.mapper.NotificationMapper;
import pbl.backend.kchi.modules.notifications.repositories.NotificationReponsitory;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;
import pbl.backend.kchi.modules.notifications.resources.NotificationResources;
import pbl.backend.kchi.modules.notifications.services.interfaces.NotificationServiceInterface;



@Tag(name="API THÔNG BÁO")
@Validated
@RestController
@RequestMapping("api/v1/notifications")
public class NotificationsController extends BaseController<
        Notifications,
        NotificationResources,
        StoreNotificationRequest,
        UpdateNotificationRequest,
        NotificationReponsitory
        > {
    public NotificationsController(
            NotificationServiceInterface service,
            NotificationMapper mapper,
            NotificationReponsitory repo
    ){
        super(service, mapper, repo, PermissionEnum.NOTIFICATIONS);
    }


}