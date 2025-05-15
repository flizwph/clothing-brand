package com.brand.backend.application.auth.service.notification;

import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис для отправки уведомлений о сбросе пароля
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetNotificationService {
    
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;
    
    /**
     * Отправляет уведомление пользователю о сбросе пароля
     */
    public void sendPasswordResetNotification(User user, String token) {
        log.debug("Отправка уведомления о сбросе пароля пользователю: {}", user.getUsername());
        
        // Формируем ссылку для сброса пароля
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        
        // Отправляем уведомление пользователю
        // В зависимости от настроек и наличия данных, выбираем канал отправки
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            sendPasswordResetEmail(user.getEmail(), resetLink);
        }
        
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            sendPasswordResetSms(user.getPhoneNumber(), resetLink);
        }
        
        if (user.getTelegramId() != null) {
            sendPasswordResetTelegram(user.getTelegramId(), resetLink);
        }
    }
    
    /**
     * Отправляет email с ссылкой для сброса пароля
     */
    private void sendPasswordResetEmail(String email, String resetLink) {
        // Здесь должен быть код для отправки email
        // В данном примере просто логируем
        log.debug("Отправка email для сброса пароля на адрес: {}", email);
        log.debug("Ссылка для сброса: {}", resetLink);
    }
    
    /**
     * Отправляет SMS с ссылкой для сброса пароля
     */
    private void sendPasswordResetSms(String phone, String resetLink) {
        // Здесь должен быть код для отправки SMS
        // В данном примере просто логируем
        log.debug("Отправка SMS для сброса пароля на номер: {}", phone);
        log.debug("Ссылка для сброса: {}", resetLink);
    }
    
    /**
     * Отправляет сообщение в Telegram с ссылкой для сброса пароля
     */
    private void sendPasswordResetTelegram(Long telegramId, String resetLink) {
        // Здесь должен быть код для отправки сообщения в Telegram
        // В данном примере просто логируем
        log.debug("Отправка Telegram сообщения для сброса пароля пользователю с ID: {}", telegramId);
        log.debug("Ссылка для сброса: {}", resetLink);
    }
} 