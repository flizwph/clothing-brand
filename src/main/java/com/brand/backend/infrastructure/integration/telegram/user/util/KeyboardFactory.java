package com.brand.backend.infrastructure.integration.telegram.user.util;

import com.brand.backend.domain.product.model.Product;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    public static ReplyKeyboardMarkup createMainMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🛍 Каталог товаров"));
        row1.add(new KeyboardButton("📋 Мои заказы"));
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("ℹ️ О нас"));
        row2.add(new KeyboardButton("📞 Контакты"));
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🔍 Поиск"));
        row3.add(new KeyboardButton("❓ Помощь"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup createProductCatalogKeyboard(List<Product> products, int page, int pageSize) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        int startIdx = page * pageSize;
        int endIdx = Math.min(startIdx + pageSize, products.size());

        for (int i = startIdx; i < endIdx; i++) {
            Product product = products.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName());
            button.setCallbackData("product:" + product.getId());
            
            row.add(button);
            keyboard.add(row);
        }

        // Добавляем навигационные кнопки
        List<InlineKeyboardButton> navigationRow = new ArrayList<>();
        
        if (page > 0) {
            InlineKeyboardButton prevButton = new InlineKeyboardButton();
            prevButton.setText("◀️ Назад");
            prevButton.setCallbackData("catalog:prev:" + (page - 1));
            navigationRow.add(prevButton);
        }
        
        if (endIdx < products.size()) {
            InlineKeyboardButton nextButton = new InlineKeyboardButton();
            nextButton.setText("Вперед ▶️");
            nextButton.setCallbackData("catalog:next:" + (page + 1));
            navigationRow.add(nextButton);
        }
        
        if (!navigationRow.isEmpty()) {
            keyboard.add(navigationRow);
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup createProductDetailKeyboard(Product product) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопки для выбора количества
        List<InlineKeyboardButton> quantityRow = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(String.valueOf(i));
            button.setCallbackData("quantity:" + product.getId() + ":" + i);
            quantityRow.add(button);
        }
        keyboard.add(quantityRow);

        // Кнопка добавления в корзину
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        InlineKeyboardButton buyButton = new InlineKeyboardButton();
        buyButton.setText("🛒 Купить сейчас");
        buyButton.setCallbackData("buy:" + product.getId());
        actionRow.add(buyButton);
        keyboard.add(actionRow);

        // Кнопка возврата в каталог
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад к каталогу");
        backButton.setCallbackData("catalog:back");
        backRow.add(backButton);
        keyboard.add(backRow);

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    public static InlineKeyboardMarkup createOrderConfirmationKeyboard() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("✅ Подтвердить заказ");
        confirmButton.setCallbackData("order:confirm");
        row.add(confirmButton);
        
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отменить");
        cancelButton.setCallbackData("order:cancel");
        row.add(cancelButton);
        
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        
        return keyboardMarkup;
    }
} 