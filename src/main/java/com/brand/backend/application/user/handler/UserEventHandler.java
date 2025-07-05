package com.brand.backend.application.user.handler;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.domain.user.event.UserEvent;
import com.brand.backend.domain.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Обработчик событий, связанных с пользователями
 * Работает без очередей с опциональным Telegram ботом
 */
@Component
@Slf4j
public class UserEventHandler {

    // Опциональная зависимость для Telegram бота
    @Autowired(required = false)
    private TelegramBotService telegramBotService;

    @Async("eventExecutor")
    @EventListener
    public void handleUserEvent(UserEvent event) {
        User user = event.getUser();
        log.debug("Обработка события пользователя: {}, тип: {}", user.getUsername(), event.getType());
        
        try {
            switch (event.getType()) {
                case REGISTERED:
                    log.info("Зарегистрирован новый пользователь: {}", user.getUsername());
                    break;
                    
                case LINKED_TELEGRAM:
                    log.info("Пользователь привязал Telegram: {}", user.getUsername());
                    notifyUserTelegramLinked(user);
                    break;
                    
                case LINKED_DISCORD:
                    log.info("Пользователь привязал Discord: {}", user.getUsername());
                    notifyUserDiscordLinked(user);
                    break;
                
                case UNLINKED_DISCORD:
                    log.info("Пользователь отвязал Discord: {}", user.getUsername());
                    break;
                    
                case VERIFIED:
                    log.info("Пользователь верифицирован: {}", user.getUsername());
                    break;
                    
                case LOGGED_IN:
                    log.info("Пользователь вошел в систему: {}", user.getUsername());
                    break;
                    
                case PASSWORD_RESET:
                    log.info("Пользователь сбросил пароль: {}", user.getUsername());
                    break;
                    
                case BALANCE_UPDATED:
                    log.info("Баланс пользователя обновлен: {}", user.getUsername());
                    break;
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события пользователя {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }
    
    private void notifyUserTelegramLinked(User user) {
        if (user.getTelegramId() != null && telegramBotService != null) {
            String message = "🔗 Ваш аккаунт успешно привязан к Telegram!\n" +
                    "Теперь вы можете делать покупки и получать уведомления о заказах через бота.";
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(user.getTelegramId().toString());
            sendMessage.setText(message);
            
            try {
                telegramBotService.execute(sendMessage);
                log.info("Уведомление о привязке Telegram отправлено пользователю: {}", user.getUsername());
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки уведомления о привязке Telegram: {}", e.getMessage());
            }
        } else if (telegramBotService == null) {
            log.debug("Telegram бот отключен, уведомление не отправлено для пользователя: {}", user.getUsername());
        }
    }
    
    private void notifyUserDiscordLinked(User user) {
        if (user.getTelegramId() != null) {
            String message = "🔗 Ваш аккаунт успешно привязан к Discord!";
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(user.getTelegramId().toString());
            sendMessage.setText(message);
            
            try {
                telegramBotService.execute(sendMessage);
                log.info("Уведомление о привязке Discord отправлено пользователю: {}", user.getUsername());
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки уведомления о привязке Discord: {}", e.getMessage());
            }
        }
    }
} 