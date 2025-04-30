package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.handlers.SubscriptionHandler;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Команда для активации подписки на десктопное приложение
 */
@Component
@RequiredArgsConstructor
public class ActivateCommand implements Command {

    private final SubscriptionHandler subscriptionHandler;

    @Override
    public void execute(Message message, TelegramBotService bot) {
        // Создаем объект Update, так как handler.handleActivateCommand ожидает Update
        Update update = new Update();
        update.setMessage(message);
        
        SendMessage response = subscriptionHandler.handleActivateCommand(update);
        bot.executeMethod(response);
    }

    @Override
    public String getCommandName() {
        return "/activate";
    }

    /**
     * Возвращает описание команды
     * @return описание команды
     */
    public String getDescription() {
        return "активация подписки и получение ссылки на десктопное приложение";
    }
} 