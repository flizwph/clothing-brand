package com.brand.backend.infrastructure.integration.telegram.admin.keyboards;

import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Класс для создания клавиатур административного бота
 */
@Component
public class AdminKeyboards {

    /**
     * Создаёт главное меню бота
     */
    public static ReplyKeyboardMarkup createMainMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);
        
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📋 Все заказы"));
        row1.add(new KeyboardButton("📊 Статистика"));
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🔍 Поиск заказа"));
        row2.add(new KeyboardButton("👤 Пользователи"));
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🎨 NFT"));
        row3.add(new KeyboardButton("🔖 Промокоды"));
        keyboard.add(row3);
        
        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("👕 Товары"));
        row4.add(new KeyboardButton("⚙️ Настройки"));
        keyboard.add(row4);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    /**
     * Создаёт клавиатуру фильтров заказов
     */
    public static InlineKeyboardMarkup createOrderFiltersKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🔄 Все", "orders:all"));
        row1.add(createButton("🆕 Новые", "orders:NEW"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("⏳ В обработке", "orders:PROCESSING"));
        row2.add(createButton("📦 Отправленные", "orders:DISPATCHED"));
        rows.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("✅ Выполненные", "orders:COMPLETED"));
        row3.add(createButton("❌ Отмененные", "orders:CANCELLED"));
        rows.add(row3);
        
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("📆 За сегодня", "orders:today"));
        row4.add(createButton("📅 За неделю", "orders:week"));
        row4.add(createButton("📅 За месяц", "orders:month"));
        rows.add(row4);
        
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        row5.add(createButton("🔍 Поиск заказа", "orders:search"));
        rows.add(row5);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру для изменения статуса заказа
     */
    public static InlineKeyboardMarkup createStatusKeyboard(Order order) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (OrderStatus status : OrderStatus.values()) {
            if (!status.equals(order.getStatus())) { // не выводим кнопку для текущего статуса
                InlineKeyboardButton button = createButton(
                    getStatusEmoji(status) + " " + status.name(),
                    "updateOrder:" + order.getId() + ":" + status.name()
                );
                rows.add(Collections.singletonList(button));
            }
        }
        
        // Добавляем кнопку для отображения деталей пользователя
        List<InlineKeyboardButton> userRow = new ArrayList<>();
        userRow.add(createButton("👤 Информация о клиенте", "viewUser:" + order.getUser().getId()));
        rows.add(userRow);
        
        // Добавляем кнопку для поиска других заказов этого пользователя
        List<InlineKeyboardButton> userOrdersRow = new ArrayList<>();
        userOrdersRow.add(createButton("🧾 Заказы клиента", "userOrders:" + order.getUser().getId()));
        rows.add(userOrdersRow);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру для страницы статистики
     */
    public static InlineKeyboardMarkup createStatisticsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("📊 Общая статистика", "stats:general"));
        row1.add(createButton("💹 Продажи по дням", "stats:daily"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("👑 Топ клиентов", "stats:topUsers"));
        row2.add(createButton("🔝 Популярные товары", "stats:topProducts"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру для управления NFT
     */
    public static InlineKeyboardMarkup createNFTKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🎨 Все NFT", "nft:all"));
        row1.add(createButton("🎁 Нераскрытые", "nft:unrevealed"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔍 Поиск по владельцу", "nft:searchByUser"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру для управления промокодами
     */
    public static InlineKeyboardMarkup createPromoCodesKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🔖 Все промокоды", "promo:all"));
        row1.add(createButton("✅ Активные", "promo:active"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("➕ Создать промокод", "promo:create"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру для деталей промокода
     */
    public static InlineKeyboardMarkup createPromoCodeDetailsKeyboard(Long promoId, boolean isActive) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("✏️ Редактировать", "promo:edit:" + promoId));
        
        if (isActive) {
            row1.add(createButton("🚫 Деактивировать", "promo:deactivate:" + promoId));
        } else {
            row1.add(createButton("✅ Активировать", "promo:activate:" + promoId));
        }
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🗑️ Удалить", "promo:delete:" + promoId));
        row2.add(createButton("◀️ Назад", "promo:all"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру для управления товарами
     */
    public static InlineKeyboardMarkup createProductsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("👕 Все товары", "product:all"));
        row1.add(createButton("🔍 Поиск товара", "product:search"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("➕ Создать товар", "product:create"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру для деталей товара
     */
    public static InlineKeyboardMarkup createProductDetailsKeyboard(Long productId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("✏️ Редактировать", "product:edit:" + productId));
        row1.add(createButton("💰 Изменить цену", "product:price:" + productId));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("📦 Обновить запасы", "product:stock:" + productId));
        row2.add(createButton("🗑️ Удалить", "product:delete:" + productId));
        rows.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("◀️ Назад", "product:all"));
        rows.add(row3);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру с кнопками назад
     */
    public static InlineKeyboardMarkup createBackKeyboard(String callback) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("◀️ Назад", callback));
        rows.add(row);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт клавиатуру с кнопками подтверждения действия
     */
    public static InlineKeyboardMarkup createConfirmKeyboard(String action, String entityId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("✅ Да", action + ":confirm:" + entityId));
        row.add(createButton("❌ Нет", action + ":cancel"));
        rows.add(row);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
    
    /**
     * Создаёт кнопку с текстом и callback-данными
     */
    private static InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
    
    /**
     * Возвращает эмодзи для статуса заказа
     */
    private static String getStatusEmoji(OrderStatus status) {
        return switch (status) {
            case NEW -> "🆕";
            case PROCESSING -> "⏳";
            case DISPATCHED -> "📦";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
    }
} 