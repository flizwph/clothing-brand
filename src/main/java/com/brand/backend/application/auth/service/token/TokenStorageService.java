package com.brand.backend.application.auth.service.token;

/**
 * Интерфейс для сервиса хранения токенов
 * Обеспечивает возможность работы с токенами в распределенной среде
 */
public interface TokenStorageService {
    
    /**
     * Добавляет токен в черный список
     */
    void blacklistToken(String token, long expirationTimeInSeconds);
    
    /**
     * Проверяет, находится ли токен в черном списке
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * Сохраняет refresh токен для пользователя
     */
    void storeRefreshToken(String username, String refreshToken, long expirationTimeInSeconds);
    
    /**
     * Получает refresh токен пользователя
     */
    String getRefreshToken(String username);
    
    /**
     * Удаляет refresh токен пользователя
     */
    void removeRefreshToken(String username);
    
    /**
     * Сохраняет версию токена пользователя
     */
    void storeTokenVersion(String username, int tokenVersion);
    
    /**
     * Получает версию токена пользователя
     */
    Integer getTokenVersion(String username);
} 