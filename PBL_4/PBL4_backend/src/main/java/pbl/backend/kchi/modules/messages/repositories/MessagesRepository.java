package pbl.backend.kchi.modules.messages.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pbl.backend.kchi.modules.messages.entities.Messages;
import pbl.backend.kchi.modules.users.entities.UserCatalogue;


@Repository
public interface MessagesRepository extends JpaRepository<Messages, Long>, JpaSpecificationExecutor<Messages> {

}