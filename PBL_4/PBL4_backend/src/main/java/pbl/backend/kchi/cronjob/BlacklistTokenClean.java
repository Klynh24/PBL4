package pbl.backend.kchi.cronjob;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pbl.backend.kchi.modules.users.repositories.BlacklistedTokenRespository;


@Service
public class BlacklistTokenClean {
    @Autowired
    private BlacklistedTokenRespository blacklistedTokenRespository;

    private static final Logger logger = LoggerFactory.getLogger(BlacklistTokenClean.class);

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")

    public void cleanupExpiredTokens() {
        LocalDateTime currentDataTime = LocalDateTime.now();
        int deleteCount = blacklistedTokenRespository.deleteByExpiryDateBefore(currentDataTime);
        logger.info("Đã xóa" + deleteCount + "token");

    }
}