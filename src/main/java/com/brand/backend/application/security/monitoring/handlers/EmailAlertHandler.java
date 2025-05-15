package com.brand.backend.application.security.monitoring.handlers;

import com.brand.backend.application.security.monitoring.AlertNotificationHandler;
import com.brand.backend.application.security.monitoring.SecurityAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Обработчик для отправки оповещений безопасности через электронную почту
 */
@Slf4j
@Component
public class EmailAlertHandler implements AlertNotificationHandler {
    
    private final JavaMailSender mailSender;
    
    @Value("${security.alert.email.admin-email:}")
    private String adminEmail;
    
    @Value("${security.alert.email.enabled:false}")
    private boolean emailAlertsEnabled;
    
    @Value("${security.alert.email.from:security-alerts@example.com}")
    private String fromEmail;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EmailAlertHandler(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public void handleAlert(SecurityAlert alert) {
        if (!emailAlertsEnabled || adminEmail == null || adminEmail.isEmpty()) {
            log.debug("Алерты через Email отключены или не настроен email админа");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("АЛЕРТ БЕЗОПАСНОСТИ: " + alert.getType());
            helper.setText(buildAlertHtmlMessage(alert), true);
            
            mailSender.send(message);
            log.debug("Алерт безопасности отправлен через Email: {}", alert.getType());
        } catch (Exception e) {
            log.error("Ошибка при отправке алерта через Email: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Формирует HTML-сообщение для отправки через email
     */
    private String buildAlertHtmlMessage(SecurityAlert alert) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2 style='color: red;'>АЛЕРТ БЕЗОПАСНОСТИ</h2>");
        html.append("<p><strong>Тип:</strong> ").append(alert.getType()).append("</p>");
        html.append("<p><strong>Время:</strong> ").append(FORMATTER.format(alert.getTimestamp())).append("</p>");
        html.append("<p><strong>Сообщение:</strong> ").append(alert.getMessage()).append("</p>");
        
        if (alert.getDetails() != null && !alert.getDetails().isEmpty()) {
            html.append("<h3>Детали:</h3>");
            html.append("<ul>");
            for (Map.Entry<String, Object> entry : alert.getDetails().entrySet()) {
                html.append("<li><strong>").append(entry.getKey()).append(":</strong> ")
                        .append(entry.getValue()).append("</li>");
            }
            html.append("</ul>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
} 