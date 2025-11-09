package pbl.backend.kchi.modules.notifications.services.interfaces;

import org.springframework.data.domain.Page;
import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;


import java.util.Map;

public interface NotificationServiceInterface {
      Notifications create(StoreNotificationRequest request);
      Notifications update(Long id, UpdateNotificationRequest request);
      Page<Notifications> paginate(Map<String, String[]> parameters );
}
