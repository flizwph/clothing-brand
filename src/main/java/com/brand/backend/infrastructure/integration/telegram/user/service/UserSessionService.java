package com.brand.backend.infrastructure.integration.telegram.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления пользовательскими сессиями
 */
@Service
@Slf4j
public class UserSessionService {
    
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    /**
     * Создает новую сессию пользователя
     * 
     * @param userId ID пользователя
     * @param state состояние сессии
     */
    public void createSession(Long userId, String state) {
        createSession(userId.toString(), state);
    }
    
    /**
     * Создает новую сессию пользователя со строковым ID
     * 
     * @param userId строковый ID пользователя
     * @param state состояние сессии
     */
    public void createSession(String userId, String state) {
        userSessions.put(userId, state);
    }
    
    /**
     * Проверяет, находится ли пользователь в указанном состоянии
     * 
     * @param userId ID пользователя
     * @param state состояние для проверки
     * @return true, если пользователь в указанном состоянии
     */
    public boolean isInState(String userId, String state) {
        return state.equals(userSessions.get(userId));
    }
    
    /**
     * Удаляет сессию пользователя
     * 
     * @param userId ID пользователя
     */
    public void clearSession(String userId) {
        userSessions.remove(userId);
    }
    
    /**
     * Возвращает текущее состояние пользователя
     * 
     * @param userId ID пользователя
     * @return текущее состояние или null
     */
    public String getUserState(String userId) {
        return userSessions.get(userId);
    }
    
    /**
     * Устанавливает состояние пользователя
     * 
     * @param userId ID пользователя
     * @param state новое состояние
     */
    public void setUserState(Long userId, String state) {
        userSessions.put(userId.toString(), state);
    }
    
    /**
     * Устанавливает состояние пользователя c использованием строкового ID
     * 
     * @param userId строковый ID пользователя
     * @param state новое состояние
     */
    public void setUserState(String userId, String state) {
        userSessions.put(userId, state);
    }
    
    /**
     * Находит первого пользователя в указанном состоянии
     * 
     * @param state состояние для поиска
     * @return Optional с ID пользователя или пустой Optional, если пользователь не найден
     */
    public Optional<String> findUserByState(String state) {
        return userSessions.entrySet().stream()
                .filter(entry -> state.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }
} 