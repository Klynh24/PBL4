package pbl.backend.kchi.modules.notifications.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.repositories.NotificationResponsitory;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.services.interfaces.NotificationServiceInterface;
import pbl.backend.kchi.services.BaseService;

@Service
public class NotificationService extends BaseService implements NotificationServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationResponsitory notificationRepository;

    @Override
    @Transactional
    public Notifications create(StoreNotificationRequest request) {
        try {
            Notifications payload = Notifications.builder()
                    .type(request.getType())
                    .message(request.getMessage())
                    .userId(request.getUserId())
                    .readStatus(request.getReadStatus())
                    .build();
            return notificationRepository.save(payload);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed" + e.getMessage());
        }

    }



}
