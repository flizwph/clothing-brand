package com.brand.backend.infrastructure.security.jwt;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.cache.annotation.Cacheable;

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

    @Cacheable(value = "tokenCache", key = "#token")
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
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
}
