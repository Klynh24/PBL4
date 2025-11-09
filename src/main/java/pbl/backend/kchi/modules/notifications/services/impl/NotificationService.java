package pbl.backend.kchi.modules.notifications.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.repositories.NotificationResponsitory;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;
import pbl.backend.kchi.modules.notifications.services.interfaces.NotificationServiceInterface;
import pbl.backend.kchi.services.BaseService;

import javax.persistence.EntityNotFoundException;
import java.util.Map;

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

    @Override
    public Page<Notifications> paginate(Map<String, String[]> parameters) {
        int page = parameters.containsKey("page") ? Integer.parseInt(parameters.get("page")[0]) : 1;
        int perpage = parameters.containsKey("perpage") ? Integer.parseInt(parameters.get("perpage")[0]) : 20;
        String sortParam = parameters.containsKey("sort") ? parameters.get("sort")[0] : null;
        Sort sort = createSort(sortParam);
        Pageable pageable = PageRequest.of(page - 1, perpage, sort);
        return notificationRepository.findAll(pageable);

    }

    @Override
    @Transactional
    public  Notifications update(Long id, UpdateNotificationRequest request) {
        Notifications notifications = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Thông báo không tồn tại"));
        Notifications payload = notifications.toBuilder()
                .readStatus(request.getReadStatus())
                .build();

        return notificationRepository.save(payload);

    }



}
