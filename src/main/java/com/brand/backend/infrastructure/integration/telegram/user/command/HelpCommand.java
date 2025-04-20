package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Команда для обработки /help
 */
@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {

    @Override
    public void execute(Message message, TelegramBotService bot) {
        String chatId = String.valueOf(message.getChatId());
        String helpText = """
                📋 *Доступные команды:*
                
                🛍️ *Покупки:*
                /buy - Купить одежду
                /cart - Посмотреть корзину
                /catalog - Просмотр категорий товаров
                
                💻 *Приложения:*
                /buyDesktop - Купить desktop-приложение
                
                🔗 *Аккаунты:*
                /linkTelegram - Привязать Telegram-аккаунт
                /linkDiscord - Привязать Discord-аккаунт
                
                ℹ️ *Информация:*
                /help - Показать это сообщение
                /about - О нашем бренде
                /contact - Контактная информация
                """;
        
        bot.sendMessage(chatId, helpText);
    }

    @Override
    public String getCommandName() {
        return "/help";
    }
} 