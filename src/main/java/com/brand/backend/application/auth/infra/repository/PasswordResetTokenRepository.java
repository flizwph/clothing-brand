package com.brand.backend.application.auth.infra.repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Репозиторий для хранения и проверки токенов восстановления пароля
 */
public interface PasswordResetTokenRepository {
    
    /**
     * Создает и сохраняет новый токен для восстановления пароля
     * 
     * @param username имя пользователя
     * @param expirationMinutes время жизни токена в минутах
     * @return токен восстановления пароля
     */
    String createToken(String username, int expirationMinutes);
    
    /**
     * Проверяет валидность токена и возвращает связанное с ним имя пользователя
     * 
     * @param token токен восстановления пароля
     * @return имя пользователя, если токен валиден, иначе null
     */
    String getUsernameByToken(String token);
    
    /**
     * Проверяет валидность токена и возвращает связанное с ним имя пользователя
     * Метод для обратной совместимости
     * 
     * @param token токен восстановления пароля
     * @return Optional с именем пользователя, если токен валиден, иначе пустой Optional
     */
    default Optional<String> validateToken(String token) {
        String username = getUsernameByToken(token);
        return username != null ? Optional.of(username) : Optional.empty();
    }
    
    /**
     * Удаляет токен после использования
     * 
     * @param token токен восстановления пароля
     */
    void removeToken(String token);
    
    /**
     * Проверяет, существует ли активный токен восстановления для пользователя
     * 
     * @param username имя пользователя
     * @return true, если токен существует и активен
     */
    boolean hasActiveToken(String username);
    
    /**
     * Очищает просроченные токены
     */
    void cleanExpiredTokens();
} 