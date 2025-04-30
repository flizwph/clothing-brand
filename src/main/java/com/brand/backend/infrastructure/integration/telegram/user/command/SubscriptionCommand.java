package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.handlers.SubscriptionHandler;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Команда для управления подписками на десктопное приложение
 */
@Component("telegramSubscriptionCommand")
@RequiredArgsConstructor
public class SubscriptionCommand implements Command {

    private final SubscriptionHandler subscriptionHandler;

    @Override
    public void execute(Message message, TelegramBotService bot) {
        // Создаем объект Update, так как handler.handleSubscriptionCommand ожидает Update
        Update update = new Update();
        update.setMessage(message);
        
        SendMessage response = subscriptionHandler.handleSubscriptionCommand(update);
        bot.executeMethod(response);
    }

    @Override
    public String getCommandName() {
        return "/subscription";
    }

    /**
     * Возвращает описание команды
     * @return описание команды
     */
    public String getDescription() {
        return "управление подписками на десктопное приложение";
    }
} 