package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.infrastructure.integration.telegram.user.service.UserSessionService;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Команда для поиска пользователей
 */
public class SearchUserCommand implements Command {
    
    private final UserSessionService userSessionService;
    
    public SearchUserCommand(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }
    
    @Override
    public String getCommandName() {
        return "/search_user";
    }
    
    @Override
    public void execute(Message message, TelegramBotService bot) {
        long chatId = message.getChatId();
        userSessionService.createSession(chatId, "SEARCH_USER");
        
        // Отправляем инструкцию пользователю
        String searchInstructions = "🔍 *Поиск пользователя*\n\n" +
                "Для поиска пользователя отправьте:\n" +
                "- Имя пользователя\n" +
                "- Email\n" +
                "- Номер телефона\n\n" +
                "Система найдет и покажет результаты.";
        
        bot.sendMessage(String.valueOf(chatId), searchInstructions);
    }
} 