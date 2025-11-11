package pbl.backend.kchi.modules.notifications.services.interfaces;

import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;
import pbl.backend.kchi.services.interfaces.BaseServiceInterface;




public interface NotificationServiceInterface extends BaseServiceInterface<Notifications, StoreNotificationRequest, UpdateNotificationRequest> {

}