package com.brand.backend.application.auth.infra.repository.impl;

import com.brand.backend.application.auth.infra.repository.PasswordResetTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory реализация репозитория токенов сброса пароля
 * Используется как основная реализация
 */
@Repository
@Slf4j
public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {
    
    // Маппинг token -> username
    private final Map<String, String> tokenToUsername = new ConcurrentHashMap<>();
    
    // Маппинг username -> token
    private final Map<String, String> usernameToToken = new ConcurrentHashMap<>();
    
    // Маппинг token -> expiration time
    private final Map<String, LocalDateTime> tokenExpirations = new ConcurrentHashMap<>();

    @Override
    public String createToken(String username, int expirationMinutes) {
        // Удаляем старый токен, если он существует
        if (usernameToToken.containsKey(username)) {
            String oldToken = usernameToToken.get(username);
            tokenToUsername.remove(oldToken);
            tokenExpirations.remove(oldToken);
        }
        
        // Создаем новый токен
        String token = UUID.randomUUID().toString();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(expirationMinutes);
        
        // Сохраняем токен
        tokenToUsername.put(token, username);
        usernameToToken.put(username, token);
        tokenExpirations.put(token, expirationTime);
        
        log.debug("Создан токен сброса пароля для пользователя {} с истечением {}", 
                username, expirationTime);
                
        return token;
    }

    @Override
    public String getUsernameByToken(String token) {
        // Проверяем, не истек ли токен
        if (isTokenExpired(token)) {
            log.debug("Токен {} истек", token);
            removeToken(token);
            return null;
        }
        
        return tokenToUsername.get(token);
    }

    @Override
    public void removeToken(String token) {
        String username = tokenToUsername.get(token);
        if (username != null) {
            tokenToUsername.remove(token);
            usernameToToken.remove(username);
            tokenExpirations.remove(token);
            log.debug("Удален токен сброса пароля для пользователя {}", username);
        }
    }

    @Override
    public boolean hasActiveToken(String username) {
        if (!usernameToToken.containsKey(username)) {
            return false;
        }
        
        String token = usernameToToken.get(username);
        if (isTokenExpired(token)) {
            removeToken(token);
            return false;
        }
        
        return true;
    }

    @Override
    public void cleanExpiredTokens() {
        log.debug("Очистка просроченных токенов");
        LocalDateTime now = LocalDateTime.now();
        
        // Находим и удаляем все просроченные токены
        tokenExpirations.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(now))
                .map(Map.Entry::getKey)
                .forEach(this::removeToken);
    }
    
    private boolean isTokenExpired(String token) {
        LocalDateTime expirationTime = tokenExpirations.get(token);
        return expirationTime == null || expirationTime.isBefore(LocalDateTime.now());
    }
} 