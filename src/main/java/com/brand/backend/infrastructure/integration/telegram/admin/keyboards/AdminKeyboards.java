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
    public static InlineKeyboardMarkup createMainMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Первый ряд
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("📦 Заказы", "menu:orders"));
        row1.add(createButton("👥 Пользователи", "menu:users"));
        rows.add(row1);
        
        // Второй ряд
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🎟 Промокоды", "menu:promocodes"));
        row2.add(createButton("🛍 Товары", "menu:products"));
        rows.add(row2);
        
        // Третий ряд
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("💰 Пополнения", "menu:deposits"));
        row3.add(createButton("🔍 Поиск заказа", "menu:search"));
        rows.add(row3);
        
        // Четвертый ряд
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("⚙️ Настройки", "menu:settings"));
        rows.add(row4);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру фильтров заказов
     */
    public static InlineKeyboardMarkup createOrderFiltersKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🔄 Все", "filter:all"));
        row1.add(createButton("🆕 Новые", "filter:NEW"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("⏳ В обработке", "filter:PROCESSING"));
        row2.add(createButton("📦 Отправленные", "filter:DISPATCHED"));
        rows.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("✅ Выполненные", "filter:COMPLETED"));
        row3.add(createButton("❌ Отмененные", "filter:CANCELLED"));
        rows.add(row3);
        
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("📆 За сегодня", "filter:today"));
        row4.add(createButton("📅 За неделю", "filter:week"));
        row4.add(createButton("📅 За месяц", "filter:month"));
        rows.add(row4);
        
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        row5.add(createButton("🔍 Поиск заказа", "filter:search"));
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
     * Создаёт клавиатуру для управления пользователями
     */
    public static InlineKeyboardMarkup createUsersMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("👥 Список пользователей", "listUsers"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔍 Поиск по строке", "searchUser"));
        rows.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("👤 Поиск по имени", "searchUserByName"));
        rows.add(row3);
        
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("✉️ Поиск по email", "searchUserByEmail"));
        rows.add(row4);
        
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        row5.add(createButton("📱 Поиск по телефону", "searchUserByPhone"));
        rows.add(row5);
        
        List<InlineKeyboardButton> row6 = new ArrayList<>();
        row6.add(createButton("◀️ Главное меню", "menu"));
        rows.add(row6);
        
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

    /**
     * Создаёт клавиатуру для поиска пользователей
     */
    public static InlineKeyboardMarkup createUserSearchKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🔍 Поиск по имени", "user:searchByName"));
        row1.add(createButton("📧 Поиск по email", "user:searchByEmail"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("📱 Поиск по телефону", "user:searchByPhone"));
        row2.add(createButton("👥 Все пользователи", "menu:users"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру для управления пополнениями
     */
    public static InlineKeyboardMarkup createDepositsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("⏳ Ожидающие", "deposits:pending"));
        row1.add(createButton("✅ Завершенные", "deposits:completed"));
        row1.add(createButton("❌ Отклоненные", "deposits:rejected"));
        rows.add(row1);
        
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("📆 За сегодня", "deposits:today"));
        row2.add(createButton("📅 За неделю", "deposits:week"));
        row2.add(createButton("📅 За месяц", "deposits:month"));
        rows.add(row2);
        
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("◀️ Назад в меню", "menu:main"));
        rows.add(row3);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру для настроек
     */
    public static InlineKeyboardMarkup createSettingsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("◀️ Назад в меню", "menu:main"));
        rows.add(row1);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
} 