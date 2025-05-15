package com.brand.backend.infrastructure.security.audit;

/**
 * Типы событий безопасности для аудита
 */
public enum SecurityEventType {
    // События аутентификации
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    
    // События учетных записей
    PASSWORD_CHANGE,
    PASSWORD_RESET_INITIATED,
    PASSWORD_RESET_COMPLETED,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    ACCOUNT_DISABLED,
    
    // События авторизации
    ACCESS_DENIED,
    ROLE_CHANGE,
    PERMISSION_CHANGE,
    
    // События токенов
    TOKEN_REFRESH,
    TOKEN_INVALIDATED,
    TOKEN_VALIDATION_FAILURE,
    
    // События безопасности
    SUSPICIOUS_ACTIVITY,
    BRUTE_FORCE_ATTEMPT,
    IP_BLOCKED,
    
    // Административные события
    USER_CREATED,
    USER_DELETED,
    SECURITY_CONFIG_CHANGE
} 