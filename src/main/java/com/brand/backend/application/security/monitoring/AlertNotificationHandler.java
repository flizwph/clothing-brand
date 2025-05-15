package com.brand.backend.application.security.monitoring;

/**
 * Интерфейс для обработчиков оповещений безопасности
 */
public interface AlertNotificationHandler {
    
    /**
     * Обрабатывает оповещение безопасности
     * 
     * @param alert оповещение безопасности
     */
    void handleAlert(SecurityAlert alert);
} 