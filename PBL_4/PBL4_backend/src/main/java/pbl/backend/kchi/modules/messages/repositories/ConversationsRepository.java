package pbl.backend.kchi.modules.messages.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pbl.backend.kchi.modules.messages.entities.Conversation;


@Repository
public interface ConversationsRepository extends JpaRepository<Conversation, Long>, JpaSpecificationExecutor<Conversation> {

}