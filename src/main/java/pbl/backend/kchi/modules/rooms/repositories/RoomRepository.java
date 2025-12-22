package pbl.backend.kchi.modules.rooms.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pbl.backend.kchi.modules.rooms.entities.Room;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
Optional<Room> findByClassesIdAndStatus(Long classId, String status);
}
