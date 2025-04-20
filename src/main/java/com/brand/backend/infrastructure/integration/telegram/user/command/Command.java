package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Интерфейс для команд Telegram бота
 */
public interface Command {
    
    /**
     * Возвращает имя команды
     * 
     * @return имя команды
     */
    String getCommandName();
    
    /**
     * Выполняет команду
     * 
     * @param message сообщение от пользователя
     * @param bot экземпляр бота
     */
    void execute(Message message, TelegramBotService bot);
} 