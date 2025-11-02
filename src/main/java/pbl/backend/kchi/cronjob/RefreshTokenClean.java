package pbl.backend.kchi.cronjob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.refresh_tokens.repositories.RefreshtokensRepository;
import org.springframework.beans.factory.annotation.Autowired;
import pbl.backend.kchi.modules.users.services.impl.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;


@Service
public class RefreshTokenClean {
    @Autowired
    private RefreshtokensRepository refreshtokensRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")

    public void cleanupExpiredTokens() {
        LocalDateTime currentDataTime = LocalDateTime.now();
        int deleteCount = refreshtokensRepository.deleteByExpiryDateBefore(currentDataTime);
        logger.info("Đã xóa" + deleteCount + "token");

    }
}
