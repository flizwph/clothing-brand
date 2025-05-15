package com.brand.backend.infrastructure.security.audit;

/**
 * Уровни важности событий безопасности
 */
public enum SecurityEventSeverity {
    /**
     * Информационное событие
     */
    INFO,
    
    /**
     * Предупреждение
     */
    WARNING,
    
    /**
     * Критическое событие
     */
    CRITICAL
} 