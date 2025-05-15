package com.brand.backend.application.auth.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для отправки уведомлений через Telegram
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService {
    
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    
    @Value("${telegram.bot.token:}")
    private String botToken;
    
    @Value("${telegram.bot.recovery.enabled:false}")
    private boolean recoveryEnabled;
    
    /**
     * Отправляет код восстановления пароля пользователю через Telegram
     * 
     * @param username имя пользователя
     * @param resetCode код восстановления пароля
     * @return true, если сообщение отправлено успешно
     */
    public boolean sendPasswordResetCode(String username, String resetCode) {
        if (!recoveryEnabled || botToken.isEmpty()) {
            log.warn("Отправка кодов восстановления через Telegram отключена");
            return false;
        }
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getTelegramChatId() == null) {
            log.warn("Невозможно отправить код восстановления для пользователя {}: отсутствует привязка к Telegram", username);
            return false;
        }
        
        try {
            String chatId = user.getTelegramChatId();
            String message = String.format(
                    "Ваш код для восстановления пароля: %s\n\n" +
                    "Этот код действителен в течение 30 минут. Если вы не запрашивали восстановление пароля, просто проигнорируйте это сообщение.",
                    resetCode);
                    
            return sendTelegramMessage(chatId, message);
        } catch (Exception e) {
            log.error("Ошибка при отправке кода восстановления пароля через Telegram: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Отправляет уведомление о смене пароля пользователю через Telegram
     * 
     * @param username имя пользователя
     * @return true, если сообщение отправлено успешно
     */
    public boolean sendPasswordChangedNotification(String username) {
        if (!recoveryEnabled || botToken.isEmpty()) {
            return false;
        }
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getTelegramChatId() == null) {
            return false;
        }
        
        try {
            String chatId = user.getTelegramChatId();
            String message = "Ваш пароль был успешно изменен. Если это были не вы, пожалуйста, немедленно свяжитесь с администратором.";
            
            return sendTelegramMessage(chatId, message);
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления о смене пароля через Telegram: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Отправляет сообщение администратору системы
     * 
     * @param adminChatId идентификатор чата администратора
     * @param message текст сообщения
     * @return true, если сообщение отправлено успешно
     */
    public boolean sendAdminMessage(String adminChatId, String message) {
        if (botToken.isEmpty()) {
            log.warn("Отправка сообщений через Telegram отключена: botToken не настроен");
            return false;
        }
        
        try {
            return sendTelegramMessage(adminChatId, message);
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения администратору: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Отправляет сообщение в Telegram
     * 
     * @param chatId ID чата пользователя в Telegram
     * @param message текст сообщения
     * @return true, если сообщение отправлено успешно
     */
    private boolean sendTelegramMessage(String chatId, String message) {
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
            
            Map<String, Object> request = new HashMap<>();
            request.put("chat_id", chatId);
            request.put("text", message);
            request.put("parse_mode", "Markdown");
            
            restTemplate.postForObject(url, request, Object.class);
            log.debug("Сообщение успешно отправлено в Telegram, chat_id: {}", chatId);
            
            return true;
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения в Telegram: {}", e.getMessage(), e);
            return false;
        }
    }
} 