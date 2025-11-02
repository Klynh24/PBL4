package pbl.backend.kchi.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.config.JwtConfig;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import io.jsonwebtoken.security.Keys;
import pbl.backend.kchi.modules.users.services.impl.UserService;
import java.util.function.Function;
import java.util.UUID;

import io.jsonwebtoken.ExpiredJwtException;
import pbl.backend.kchi.modules.users.repositories.BlacklistedTokenRespository;
import pbl.backend.kchi.modules.refresh_tokens.entities.Refresh_tokens;
import pbl.backend.kchi.modules.refresh_tokens.repositories.RefreshtokensRepository;


@Service

public class JwtService {

    private final JwtConfig jwtConfig;
    private final Key key;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private BlacklistedTokenRespository blacklistedTokenRespository;

    @Autowired
    private RefreshtokensRepository refreshtokensRepository;

    public JwtService (
            JwtConfig jwtConfig
    ) {
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
//    private <T> T extractClaim(String token,java.util.function.Function<Claims, T> claimsResolver) {
//        final Claims claims = extractAllClaims(token);
//        return claimsResolver.apply(claims);
//    }
//    public String extractUsername(String token) {
//
//        return extractClaim(token, Claims::getSubject);
//    }
//
//
//    private Claims extractAllClaims(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//    }
    public String getUserIdFromJwt(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();

    }

    public String getEmailFromJwt(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.get("email", String.class);
    }



    public boolean isTokenFormsValid(String token) {
        try {
            String[] tokenParts = token.split("\\.");
            return tokenParts.length == 3;

        } catch (Exception e) {
            return false;
        }

    }

    public boolean isSignatureValid(String token) {
        try {
     //       logger.info(token);
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Key getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes();
        return Keys.hmacShaKeyFor(Base64.getEncoder().encode(keyBytes));
    }

    public boolean isTokenExpired(String token) {
       try {
           Date expiration = getClaimFromToken(token, Claims::getExpiration);
           logger.info("Expirai: {}", expiration);
           return expiration.before(new Date());
       } catch (Exception e) {
           return false;
       }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Claims getALlClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return null;
        }
    }

    public String generateRefreshToken(Long userId, String email) {
        logger.info("Generating refresh token.....");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshTokenExpirationTime());

        String refreshToken = UUID.randomUUID().toString();

        LocalDateTime localExpiryDate = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        Optional<Refresh_tokens> optionalRefreshTokens = refreshtokensRepository.findByUserId(userId);

        if(optionalRefreshTokens.isPresent()) {
            Refresh_tokens dBRefreshToken = optionalRefreshTokens.get();
            dBRefreshToken.setRefreshToken(refreshToken);
            dBRefreshToken.setExpiryDate(localExpiryDate);
            refreshtokensRepository.save(dBRefreshToken);

        } else {
            Refresh_tokens insertToken = new Refresh_tokens();
            insertToken.setRefreshToken(refreshToken);
            insertToken.setExpiryDate(localExpiryDate);
            insertToken.setUserId(userId);

            refreshtokensRepository.save(insertToken);
        }
        return refreshToken;
    }

    public<T> T getClaimFromToken(String token,java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isIssuerToken(String token) {
        String tokenIssuer = getClaimFromToken(token, Claims::getIssuer);
        return jwtConfig.getIssuer().equals(tokenIssuer);
    }

    public boolean isBlacklistedToken(String token) {
        return blacklistedTokenRespository.existsByToken(token);
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
