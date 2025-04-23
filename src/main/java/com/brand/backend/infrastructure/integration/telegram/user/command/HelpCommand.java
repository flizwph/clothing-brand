package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Команда для отображения помощи по боту
 */
public class HelpCommand implements Command {

    @Override
    public void execute(Message message, TelegramBotService bot) {
        String helpMessage = """
                Доступные команды:
                
                /buy - Купить одежду
                /cart - Корзина покупок
                /subscription - Управление подписками на десктопное приложение
                /activate - Активация подписки и получение ссылки на скачивание приложения
                /linkTelegram - Привязать Telegram аккаунт
                /linkDiscord - Привязать Discord аккаунт
                /help - Помощь
                
                Также вы можете использовать кнопки меню для навигации.
                """;
        bot.sendMessage(message.getChatId().toString(), helpMessage);
    }

    @Override
    public String getCommandName() {
        return "/help";
    }
} 