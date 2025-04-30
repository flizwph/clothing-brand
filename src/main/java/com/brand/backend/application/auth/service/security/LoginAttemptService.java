package com.brand.backend.application.auth.service.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для отслеживания попыток входа в систему и блокировки в случае brute-force
 */
@Slf4j
@Service
public class LoginAttemptService {
    
    private final HttpServletRequest request;
    
    private final int maxAttempts;
    
    private final int blockDurationMinutes;
    
    private final LoadingCache<String, Integer> attemptsCache;
    
    /**
     * Инициализация кэша
     */
    public LoginAttemptService(HttpServletRequest request, 
                              @Value("${security.login.max-attempts:5}") int maxAttempts,
                              @Value("${security.login.block-duration-minutes:30}") int blockDurationMinutes) {
        this.request = request;
        this.maxAttempts = maxAttempts;
        this.blockDurationMinutes = blockDurationMinutes;
        
        this.attemptsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(blockDurationMinutes, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }
    
    /**
     * Получить IP-адрес клиента из запроса
     */
    private String getClientIP() {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    
    /**
     * Получить ключ для кэша (комбинация IP и имени пользователя)
     */
    private String getCacheKey(String username) {
        String ip = getClientIP();
        return ip + ":" + username;
    }
    
    /**
     * Зарегистрировать неудачную попытку входа
     */
    public void loginFailed(String username) {
        String key = getCacheKey(username);
        int attempts;
        try {
            attempts = attemptsCache.get(key);
            attempts++;
            attemptsCache.put(key, attempts);
            log.warn("Неудачная попытка входа #{} для пользователя {} с IP {}", 
                    attempts, username, getClientIP());
        } catch (ExecutionException e) {
            attempts = 0;
            log.error("Ошибка при обновлении счетчика попыток: {}", e.getMessage());
        }
    }
    
    /**
     * Зарегистрировать успешный вход (сбросить счетчик)
     */
    public void loginSucceeded(String username) {
        String key = getCacheKey(username);
        attemptsCache.invalidate(key);
        log.info("Успешный вход для пользователя {} с IP {}, счетчик попыток сброшен", 
                username, getClientIP());
    }
    
    /**
     * Проверить, заблокирован ли пользователь
     */
    public boolean isBlocked(String username) {
        String key = getCacheKey(username);
        try {
            return attemptsCache.get(key) >= maxAttempts;
        } catch (ExecutionException e) {
            return false;
        }
    }
    
    /**
     * Получить количество оставшихся попыток
     */
    public int getRemainingAttempts(String username) {
        String key = getCacheKey(username);
        try {
            int attempts = attemptsCache.get(key);
            return Math.max(0, maxAttempts - attempts);
        } catch (ExecutionException e) {
            return maxAttempts;
        }
    }
} 