package pbl.backend.kchi.modules.notifications.mapper;

import org.mapstruct.*;
import pbl.backend.kchi.annotations.BaseMapperAnnotation;
import pbl.backend.kchi.mapper.BaseMapper;
// import pbl.backend.kchi.modules.classes.entities.Classes; // <-- Đã xóa import thừa

import pbl.backend.kchi.modules.notifications.entities.Notifications;
import pbl.backend.kchi.modules.notifications.requests.StoreNotificationRequest;
import pbl.backend.kchi.modules.notifications.requests.UpdateNotificationRequest;
import pbl.backend.kchi.modules.notifications.resources.NotificationResources;

@Mapper(componentModel = "spring")

public interface NotificationMapper extends BaseMapper<Notifications, NotificationResources, StoreNotificationRequest, UpdateNotificationRequest> {

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Notifications toEntity(StoreNotificationRequest createNotificationRequest);

    @Override
    @BaseMapperAnnotation
    @Mapping(target = "user", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdateNotificationRequest updateNotificationRequest, @MappingTarget Notifications entity);
}