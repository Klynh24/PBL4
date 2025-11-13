package pbl.backend.kchi.modules.notifications.services.impl;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;



import pbl.backend.kchi.modules.notifications.entities.Notifications;

import pbl.backend.kchi.modules.notifications.mapper.NotificationMapper;
import pbl.backend.kchi.modules.notifications.repositories.NotificationReponsitory;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;
import pbl.backend.kchi.modules.notifications.services.interfaces.NotificationServiceInterface;
import pbl.backend.kchi.services.BaseService;


@Service
public class NotificationService extends BaseService<
        Notifications,
        NotificationMapper,
        StoreNotificationRequest,
        UpdateNotificationRequest,
        NotificationReponsitory
        > implements NotificationServiceInterface {

    private final NotificationMapper notificationMapper;

    @Autowired
    private NotificationReponsitory notificationRepository;

    public NotificationService(
            NotificationMapper notificationMapper
    ){
        this.notificationMapper = notificationMapper;
    }

    @Override
    protected String[] getSearchFields(){
        return new String[]{"name"};
    }

    @Override
    protected String[] getRelations(){
        return new String[]{"permissions"};
    }

    @Override
    protected NotificationReponsitory getRepository(){
        return notificationRepository;
    }

    @Override
    protected NotificationMapper getMapper(){
        return notificationMapper;
    }


}