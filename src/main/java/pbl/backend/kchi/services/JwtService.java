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
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import io.jsonwebtoken.security.Keys;
import pbl.backend.kchi.modules.users.services.impl.UserService;
import java.util.function.Function;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.ExpiredJwtException;
import pbl.backend.kchi.modules.users.repositories.BlacklistedTokenRespository;


@Service

public class JwtService {

    private final JwtConfig jwtConfig;
    private final Key key;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private BlacklistedTokenRespository blacklistedTokenRespository;

    public JwtService (
            JwtConfig jwtConfig
    ) {
        this.jwtConfig = jwtConfig;
        this.key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(jwtConfig.getSecretKey().getBytes()));


    }

    public String generateToken(Long userId, String email) {
        logger.info("generating....");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpirationTime());
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
        } catch (SignatureException e) {
            return false;
        }
    }

    public Key getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes();
        return Keys.hmacShaKeyFor(Base64.getEncoder().encode(keyBytes));
    }

    public boolean isTokenExpired(String token) {
        try {

            final Date expiration = getClaimFromToken(token, Claims::getExpiration);
            return expiration.after(new Date());
        } catch (ExpiredJwtException e) {
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
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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
}
