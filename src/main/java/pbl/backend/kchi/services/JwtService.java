package pbl.backend.kchi.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.config.JwtConfig;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import io.jsonwebtoken.ExpiredJwtException;
import pbl.backend.kchi.modules.users.entities.User;
import pbl.backend.kchi.modules.users.repositories.BlacklistedTokenRepository;
import pbl.backend.kchi.modules.refresh_tokens.entities.Refresh_tokens;
import pbl.backend.kchi.modules.refresh_tokens.repositories.RefreshtokensRepository;


@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final Key key;
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private RefreshtokensRepository refreshtokensRepository;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(jwtConfig.getSecretKey().getBytes()));
    }

    public String generateToken(Long userId, String email, Long expirationTime) {
        logger.info("generating....");
        Date now = new Date();

        if(expirationTime == null) {
            expirationTime =  jwtConfig.getExpirationTime();
        }

        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public<T> T getClaimFromToken(String token,java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String getUserIdFromJwt(String token) {
        // Hàm này sẽ tự động ném ExpiredJwtException, SignatureException, v.v.
        // nếu token có vấn đề, và filter sẽ bắt được.
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.get("email", String.class);
    }

    public Claims getALlClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key) // Sửa lại: dùng key thay vì getSigningKey()
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return null;
        }
    }

    public boolean isIssuerToken(String token) {
        String tokenIssuer = getClaimFromToken(token, Claims::getIssuer);
        return jwtConfig.getIssuer().equals(tokenIssuer);
    }

    public boolean isBlacklistedToken(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    // --- CÁC HÀM REFRESH TOKEN (giữ nguyên) ---

    public String generateRefreshToken(User user, String email) {
        logger.info("Generating refresh token.....");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshTokenExpirationTime());

        String refreshToken = UUID.randomUUID().toString();

        LocalDateTime localExpiryDate = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        Optional<Refresh_tokens> optionalRefreshTokens = refreshtokensRepository.findByUserId(user.getId());

        if(optionalRefreshTokens.isPresent()) {
            Refresh_tokens dBRefreshToken = optionalRefreshTokens.get();
            dBRefreshToken.setRefreshToken(refreshToken);
            dBRefreshToken.setExpiryDate(localExpiryDate);
            refreshtokensRepository.save(dBRefreshToken);

        } else {
            Refresh_tokens insertToken = new Refresh_tokens();
            insertToken.setRefreshToken(refreshToken);
            insertToken.setExpiryDate(localExpiryDate);
            insertToken.setUser(user);

            refreshtokensRepository.save(insertToken);
        }
        return refreshToken;
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            Refresh_tokens refreshToken = refreshtokensRepository.findByRefreshToken(token).orElseThrow(() ->
                    new RuntimeException("Refresh Token không tồn tại"));
            LocalDateTime expirationLocalDateTime = refreshToken.getExpiryDate();
            Date expirationDate = Date.from(expirationLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
            return  expirationDate.after(new Date());

        } catch (Exception e) {
            return false;
        }
    }
}