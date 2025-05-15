package com.brand.backend.application.security.monitoring.handlers;

import com.brand.backend.application.auth.service.notification.TelegramNotificationService;
import com.brand.backend.application.security.monitoring.AlertNotificationHandler;
import com.brand.backend.application.security.monitoring.SecurityAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Обработчик для отправки оповещений безопасности через Telegram
 */
@Slf4j
@Component
public class TelegramAlertHandler implements AlertNotificationHandler {
    
    private final TelegramNotificationService telegramService;
    
    @Value("${security.alert.telegram.admin-chat-id:}")
    private String adminChatId;
    
    @Value("${security.alert.telegram.enabled:false}")
    private boolean telegramAlertsEnabled;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TelegramAlertHandler(TelegramNotificationService telegramService) {
        this.telegramService = telegramService;
    }
    
    @Override
    public void handleAlert(SecurityAlert alert) {
        if (!telegramAlertsEnabled || adminChatId == null || adminChatId.isEmpty()) {
            log.debug("Алерты через Telegram отключены или не настроен chat_id админа");
            return;
        }
        
        try {
            String message = buildAlertMessage(alert);
            telegramService.sendAdminMessage(adminChatId, message);
            log.debug("Алерт безопасности отправлен через Telegram: {}", alert.getType());
        } catch (Exception e) {
            log.error("Ошибка при отправке алерта через Telegram: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Формирует сообщение для отправки через Telegram
     */
    private String buildAlertMessage(SecurityAlert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 *АЛЕРТ БЕЗОПАСНОСТИ* 🚨\n\n");
        sb.append("*Тип:* ").append(alert.getType()).append("\n");
        sb.append("*Время:* ").append(FORMATTER.format(alert.getTimestamp())).append("\n");
        sb.append("*Сообщение:* ").append(alert.getMessage()).append("\n\n");
        
        if (alert.getDetails() != null && !alert.getDetails().isEmpty()) {
            sb.append("*Детали:*\n");
            for (Map.Entry<String, Object> entry : alert.getDetails().entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return sb.toString();
    }
} 