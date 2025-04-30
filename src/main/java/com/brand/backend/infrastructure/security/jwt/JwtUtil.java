package com.brand.backend.infrastructure.security.jwt;

import com.brand.backend.application.auth.service.token.TokenStorageService;
import com.brand.backend.domain.user.model.User;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenExpiration;
    
    public static final String TOKEN_VERSION_CLAIM = "token_version";
    
    private final TokenStorageService tokenStorageService;
    
    public JwtUtil(TokenStorageService tokenStorageService) {
        this.tokenStorageService = tokenStorageService;
    }

    public String generateAccessToken(String username, Integer tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_VERSION_CLAIM, tokenVersion);
        return generateToken(claims, username, accessTokenExpiration);
    }
    
    public String generateAccessToken(String username) {
        return generateAccessToken(username, 1); // По умолчанию версия 1
    }

    private String generateToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
    
    private String generateToken(String username, long expiration) {
        return generateToken(new HashMap<>(), username, expiration);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Integer extractTokenVersion(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_VERSION_CLAIM, Integer.class));
    }
    
    /**
     * Проверяет валидность токена без проверки пользователя
     * - Не истек ли срок действия
     * - Не находится ли токен в черном списке
     */
    public boolean isTokenValid(String token) {
        try {
            // Проверка на черный список
            if (tokenStorageService.isTokenBlacklisted(token)) {
                log.debug("Токен находится в черном списке");
                return false;
            }
            
            // Проверка на истечение срока действия
            Date expiration = extractClaim(token, Claims::getExpiration);
            if (expiration.before(new Date())) {
                log.debug("Токен истек");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Ошибка при проверке токена: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Проверяет валидность токена
     * - Не истек ли срок действия
     * - Соответствует ли пользователь
     * - Не находится ли токен в черном списке
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            
            // Проверка на черный список
            if (tokenStorageService.isTokenBlacklisted(token)) {
                log.debug("Токен находится в черном списке: {}", token);
                return false;
            }
            
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Ошибка при проверке токена: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Добавляет токен в черный список
     */
    public void blacklistToken(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            long expirationTimeInSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            
            if (expirationTimeInSeconds > 0) {
                tokenStorageService.blacklistToken(token, expirationTimeInSeconds);
                log.debug("Токен добавлен в черный список до {}", expiration);
            }
        } catch (Exception e) {
            log.error("Ошибка при добавлении токена в черный список: {}", e.getMessage());
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
    
    // Получение всех claims из токена (для удобства)
    public Claims getAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Проверка версии токена пользователя
     */
    public boolean validateTokenVersion(String token, User user) {
        final Claims claims = getAllClaims(token);
        Integer tokenVersion = claims.get("tokenVersion", Integer.class);
        
        // Если в токене нет версии или у пользователя нет версии, считаем версию 1
        if (tokenVersion == null) {
            tokenVersion = 1;
        }
        
        Integer userTokenVersion = user.getTokenVersion();
        if (userTokenVersion == null) {
            userTokenVersion = 1;
        }
        
        return tokenVersion.equals(userTokenVersion);
    }
}
