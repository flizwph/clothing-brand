package com.brand.backend.application.auth.service.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация сервиса хранения токенов в памяти
 * Используется как основная реализация
 */
@Slf4j
@Service
public class InMemoryTokenStorageService implements TokenStorageService {
    
    // Хранилище для блэклиста токенов
    private final Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();
    
    // Хранилище для refresh токенов
    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();
    
    // Хранилище для версий токенов
    private final Map<String, Integer> tokenVersions = new ConcurrentHashMap<>();
    
    // Хранилище для времени истечения
    private final Map<String, LocalDateTime> expirationTimes = new ConcurrentHashMap<>();

    @Override
    public void blacklistToken(String token, long expirationTimeInSeconds) {
        String key = "bl:" + token;
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(expirationTimeInSeconds);
        blacklistedTokens.put(key, expirationTime);
        log.debug("Токен добавлен в блэклист до {}", expirationTime);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        String key = "bl:" + token;
        LocalDateTime expirationTime = blacklistedTokens.get(key);
        
        if (expirationTime == null) {
            return false;
        }
        
        // Если срок действия истек, удаляем из блэклиста
        if (expirationTime.isBefore(LocalDateTime.now())) {
            blacklistedTokens.remove(key);
            return false;
        }
        
        return true;
    }

    @Override
    public void storeRefreshToken(String username, String refreshToken, long expirationTimeInSeconds) {
        String key = "rt:" + username;
        refreshTokens.put(key, refreshToken);
        
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(expirationTimeInSeconds);
        expirationTimes.put(key, expirationTime);
        
        log.debug("Refresh токен сохранен для пользователя {} до {}", username, expirationTime);
    }

    @Override
    public String getRefreshToken(String username) {
        String key = "rt:" + username;
        LocalDateTime expirationTime = expirationTimes.get(key);
        
        if (expirationTime == null || expirationTime.isBefore(LocalDateTime.now())) {
            refreshTokens.remove(key);
            expirationTimes.remove(key);
            return null;
        }
        
        return refreshTokens.get(key);
    }

    @Override
    public void removeRefreshToken(String username) {
        String key = "rt:" + username;
        refreshTokens.remove(key);
        expirationTimes.remove(key);
        log.debug("Refresh токен удален для пользователя {}", username);
    }

    @Override
    public void storeTokenVersion(String username, int tokenVersion) {
        if (username == null) {
            log.warn("Попытка сохранить версию токена для null username");
            return;
        }
        String key = "tv:" + username;
        tokenVersions.put(key, tokenVersion);
        log.debug("Версия токена {} сохранена для пользователя {}", tokenVersion, username);
    }

    @Override
    public Integer getTokenVersion(String username) {
        if (username == null) {
            log.warn("Попытка получить версию токена для null username");
            return 1;
        }
        String key = "tv:" + username;
        return tokenVersions.getOrDefault(key, 1);
    }
    
    /**
     * Периодическая очистка истекших данных
     * На практике можно добавить @Scheduled для запуска по расписанию
     */
    public void cleanExpiredData() {
        LocalDateTime now = LocalDateTime.now();
        
        // Очистка блэклистов
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        
        // Очистка истекших refresh токенов
        expirationTimes.entrySet().stream()
                .filter(entry -> entry.getValue().isBefore(now))
                .map(Map.Entry::getKey)
                .forEach(key -> {
                    refreshTokens.remove(key);
                    expirationTimes.remove(key);
                });
    }
} 