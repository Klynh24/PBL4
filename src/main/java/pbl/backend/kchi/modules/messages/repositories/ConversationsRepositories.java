package pbl.backend.kchi.modules.messages.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pbl.backend.kchi.modules.messages.entities.Conversations;


@Repository
public interface ConversationsRepositories extends JpaRepository<Conversations, Long> {
}
