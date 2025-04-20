package com.brand.backend.application.user.handler;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.domain.user.event.UserEvent;
import com.brand.backend.domain.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Обработчик событий, связанных с пользователями
 */
@Component
@RequiredArgsConstructor
public class UserEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserEventHandler.class);

    private final TelegramBotService telegramBotService;

    @Async("eventExecutor")
    @EventListener
    public void handleUserEvent(UserEvent event) {
        User user = event.getUser();
        log.debug("Обработка события пользователя: {}, тип: {}", user.getUsername(), event.getEventType());
        
        try {
            switch (event.getEventType()) {
                case REGISTERED:
                    log.info("Зарегистрирован новый пользователь: {}", user.getUsername());
                    break;
                    
                case UPDATED:
                    log.info("Пользователь обновил данные: {}", user.getUsername());
                    break;
                    
                case LINKED_TELEGRAM:
                    log.info("Пользователь привязал Telegram: {}", user.getUsername());
                    notifyUserTelegramLinked(user);
                    break;
                    
                case LINKED_DISCORD:
                    log.info("Пользователь привязал Discord: {}", user.getUsername());
                    notifyUserDiscordLinked(user);
                    break;
                    
                case VERIFIED:
                    log.info("Пользователь верифицирован: {}", user.getUsername());
                    break;
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события пользователя {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }
    
    private void notifyUserTelegramLinked(User user) {
        if (user.getTelegramId() != null) {
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