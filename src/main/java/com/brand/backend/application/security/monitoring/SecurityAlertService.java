package com.brand.backend.application.security.monitoring;

import com.brand.backend.infrastructure.security.audit.SecurityAuditEvent;
import com.brand.backend.infrastructure.security.audit.SecurityAuditRepository;
import com.brand.backend.infrastructure.security.audit.SecurityEventSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для отслеживания подозрительной активности и отправки алертов
 */
@Slf4j
@Service
public class SecurityAlertService {

    private final SecurityAuditRepository securityAuditRepository;
    private final List<AlertNotificationHandler> alertHandlers;
    
    @Value("${security.alert.login-failure-threshold:5}")
    private int loginFailureThreshold;
    
    @Value("${security.alert.time-window-minutes:10}")
    private int timeWindowMinutes;
    
    private final Map<String, Integer> failedLoginAttempts = new HashMap<>();
    
    public SecurityAlertService(SecurityAuditRepository securityAuditRepository,
                               List<AlertNotificationHandler> alertHandlers) {
        this.securityAuditRepository = securityAuditRepository;
        this.alertHandlers = alertHandlers;
    }
    
    /**
     * Планировщик для регулярной проверки подозрительной активности
     */
    @Scheduled(fixedDelayString = "${security.alert.check-interval-ms:60000}")
    public void checkSuspiciousActivities() {
        log.debug("Запуск проверки подозрительной активности");
        LocalDateTime since = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        
        List<SecurityAuditEvent> criticalEvents = securityAuditRepository
                .findBySeverityOrderByTimestampDesc(SecurityEventSeverity.CRITICAL, PageRequest.of(0, 50));
        
        // Группируем события по IP-адресу
        Map<String, List<SecurityAuditEvent>> eventsByIp = criticalEvents.stream()
                .filter(event -> event.getIpAddress() != null && !event.getIpAddress().isEmpty())
                .collect(Collectors.groupingBy(SecurityAuditEvent::getIpAddress));
        
        for (Map.Entry<String, List<SecurityAuditEvent>> entry : eventsByIp.entrySet()) {
            String ipAddress = entry.getKey();
            List<SecurityAuditEvent> events = entry.getValue();
            
            if (events.size() >= 3) {
                log.warn("Обнаружена подозрительная активность с IP {}: {} критических событий", 
                        ipAddress, events.size());
                
                sendAlert(AlertType.SUSPICIOUS_IP_ACTIVITY, 
                        String.format("Подозрительная активность с IP %s: %d критических событий за последние %d минут", 
                                ipAddress, events.size(), timeWindowMinutes),
                        Map.of("ipAddress", ipAddress, "eventCount", events.size()));
            }
        }
        
        // Проверка подозрительной активности по пользователям
        checkSuspiciousUserActivity(since);
    }
    
    /**
     * Проверяет подозрительную активность по пользователям
     */
    private void checkSuspiciousUserActivity(LocalDateTime since) {
        // Группировка событий по пользователям
        List<Object[]> userActivityStats = securityAuditRepository.findUserActivityStatsSince(since);
        
        for (Object[] stat : userActivityStats) {
            String username = (String) stat[0];
            Long failureCount = (Long) stat[1];
            
            if (failureCount >= loginFailureThreshold) {
                log.warn("Обнаружена подозрительная активность для пользователя {}: {} неудачных попыток входа", 
                        username, failureCount);
                
                sendAlert(AlertType.BRUTE_FORCE_ATTEMPT, 
                        String.format("Возможная брутфорс-атака на аккаунт %s: %d неудачных попыток входа за %d минут", 
                                username, failureCount, timeWindowMinutes),
                        Map.of("username", username, "failureCount", failureCount));
            }
        }
    }
    
    /**
     * Отправляет алерт через все зарегистрированные обработчики
     */
    public void sendAlert(AlertType alertType, String message, Map<String, Object> details) {
        SecurityAlert alert = SecurityAlert.builder()
                .timestamp(LocalDateTime.now())
                .type(alertType)
                .message(message)
                .details(details)
                .build();
        
        log.warn("Отправка алерта: {}: {}", alertType, message);
        
        for (AlertNotificationHandler handler : alertHandlers) {
            try {
                handler.handleAlert(alert);
            } catch (Exception e) {
                log.error("Ошибка при отправке алерта через {}: {}", 
                        handler.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Регистрирует неудачную попытку входа для потенциальной отправки алерта
     */
    public void registerFailedLogin(String username, String ipAddress) {
        String key = username + ":" + ipAddress;
        int attempts = failedLoginAttempts.getOrDefault(key, 0) + 1;
        failedLoginAttempts.put(key, attempts);
        
        if (attempts >= loginFailureThreshold) {
            sendAlert(AlertType.BRUTE_FORCE_ATTEMPT, 
                    String.format("Возможная брутфорс-атака: %d неудачных попыток входа для пользователя %s с IP %s", 
                            attempts, username, ipAddress),
                    Map.of("username", username, "ipAddress", ipAddress, "attempts", attempts));
            
            // Сбрасываем счетчик после отправки алерта
            failedLoginAttempts.remove(key);
        }
    }
    
    /**
     * Сбрасывает счетчик неудачных попыток входа при успешном входе
     */
    public void registerSuccessfulLogin(String username, String ipAddress) {
        String key = username + ":" + ipAddress;
        failedLoginAttempts.remove(key);
    }
} 