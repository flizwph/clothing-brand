package com.brand.backend.infrastructure.integration.telegram.user.command;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда для обработки /start
 */
@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    @Override
    public void execute(Message message, TelegramBotService bot) {
        String chatId = String.valueOf(message.getChatId());
        bot.sendMessage(chatId, "👋 Добро пожаловать в наш магазин! Используйте /help для просмотра доступных команд.", getMainMenuButtons());
    }

    @Override
    public String getCommandName() {
        return "/start";
    }

    /**
     * Создает клавиатуру для главного меню
     */
    private InlineKeyboardMarkup getMainMenuButtons() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Первый ряд кнопок
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("👕 Магазин", "shop"));
        row1.add(createButton("💬 Помощь", "help"));
        rows.add(row1);
        
        // Второй ряд кнопок - кнопки привязки аккаунтов
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔗 Привязать Telegram", "startLinkTelegram"));
        row2.add(createButton("🔗 Привязать Discord", "startLinkDiscord"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает кнопку с заданным текстом и callback-данными
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
} 