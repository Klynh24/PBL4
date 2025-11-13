package pbl.backend.kchi.modules.notifications.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pbl.backend.kchi.modules.notifications.entities.Notifications;


@Repository
public interface NotificationReponsitory extends JpaRepository<Notifications, Long>, JpaSpecificationExecutor<Notifications> {

}