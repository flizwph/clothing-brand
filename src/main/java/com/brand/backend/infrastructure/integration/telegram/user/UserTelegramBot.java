package com.brand.backend.infrastructure.integration.telegram.user;

import com.brand.backend.infrastructure.integration.telegram.user.command.CommandFactory;
import com.brand.backend.infrastructure.integration.telegram.user.handler.CallbackHandler;
import com.brand.backend.infrastructure.integration.telegram.user.handler.MessageHandler;
import com.brand.backend.infrastructure.integration.telegram.user.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class UserTelegramBot extends TelegramLongPollingBot {

    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;
    private final UserSessionService userSessionService;
    private final String botUsername;

    @Autowired
    public UserTelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            MessageHandler messageHandler,
            CallbackHandler callbackHandler,
            UserSessionService userSessionService) {
        super(botToken);
        this.botUsername = botUsername;
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
        this.userSessionService = userSessionService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                handleIncomingMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения: ", e);
            try {
                if (update.hasMessage()) {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(update.getMessage().getChatId().toString());
                    errorMessage.setText("Произошла ошибка при обработке вашего запроса. Пожалуйста, попробуйте позже.");
                    execute(errorMessage);
                }
            } catch (TelegramApiException ex) {
                log.error("Не удалось отправить сообщение об ошибке: ", ex);
            }
        }
    }

    private void handleIncomingMessage(Message message) {
        BotApiMethod<?> response = messageHandler.handle(message);
        if (response != null) {
            executeMethod(response);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        BotApiMethod<?> response = callbackHandler.handle(callbackQuery);
        if (response != null) {
            executeMethod(response);
        }
    }

    private void executeMethod(BotApiMethod<?> method) {
        try {
            execute(method);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выполнении метода API: ", e);
        }
    }
} 