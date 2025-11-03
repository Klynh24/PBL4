package pbl.backend.kchi.modules.users.services.impl;
import io.jsonwebtoken.Claims;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import pbl.backend.kchi.modules.users.repositories.BlacklistedTokenRespository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;


import pbl.backend.kchi.modules.users.entities.BlacklistedToken;
import pbl.backend.kchi.modules.users.requests.BlacklistedTokenRequest;
import pbl.backend.kchi.resources.ApiResource;
import pbl.backend.kchi.services.JwtService;
import pbl.backend.kchi.resources.MessageResource;

@Service
public class BlacklistService {

    @Autowired
    private BlacklistedTokenRespository blacklistedTokenRespository;

    @Autowired
    private JwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    public Object create(BlacklistedTokenRequest request) {
        try {
            if(blacklistedTokenRespository.existsByToken(request.getToken())) {
                return ApiResource.error("TOKEN-ERROR","Token đã tồn tại trong database", HttpStatus.BAD_REQUEST);
            }
            logger.info(request.getToken());

            Claims claims = jwtService.getALlClaimsFromToken(request.getToken());

            Long userId = Long.valueOf(claims.getSubject());

            Date expiryDate = claims.getExpiration();

            BlacklistedToken blacklistedToken = new BlacklistedToken();
            blacklistedToken.setToken(request.getToken());
            blacklistedToken.setUserId(userId);
            blacklistedToken.setExpiryDate(expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            blacklistedTokenRespository.save(blacklistedToken);

            logger.info("Thêm token vào danh sách blacklist thành công");
            return new MessageResource("Thêm token vào blacklist thành công");


        } catch (Exception e) {
            return new MessageResource("NetworkError!" + e.getMessage());

        }
    }
}
