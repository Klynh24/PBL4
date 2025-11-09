package pbl.backend.kchi.modules.notifications.services.interfaces;

import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.users.entities.User;


import java.util.Map;

public interface NotificationServiceInterface {
      Notifications create(StoreNotificationRequest request);
//    User update(Long id, UpdateUserRequest request);
//    Page<User> paginate(Map<String, String[]> parameters );
}
