package com.brand.backend.common.service;

import com.brand.backend.common.filter.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Сервис для трассировки запросов
 * Используется для логирования событий безопасности с RequestId для удобного отслеживания
 */
@Slf4j
@Service
public class RequestTraceService {
    
    /**
     * Логирует событие аутентификации
     */
    public void logAuthEvent(String event, String username, String clientIp) {
        log.info("AUTH_EVENT: {} для пользователя {} с IP {}", event, username, clientIp);
    }
    
    /**
     * Логирует событие безопасности
     */
    public void logSecurityEvent(String event, String details) {
        log.info("SECURITY_EVENT: {} - {}", event, details);
    }
    
    /**
     * Логирует событие с высоким приоритетом (подозрительная активность)
     */
    public void logAlertEvent(String event, String details) {
        log.warn("ALERT_EVENT: {} - {}", event, details);
    }
    
    /**
     * Получает текущий RequestId из MDC
     */
    public String getCurrentRequestId() {
        return MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
    }
} 