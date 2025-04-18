package com.brand.backend.infrastructure.integration.telegram.admin.handlers;

import com.brand.backend.infrastructure.integration.telegram.admin.dto.OrderStatisticsDto;
import com.brand.backend.infrastructure.integration.telegram.admin.keyboards.AdminKeyboards;
import com.brand.backend.infrastructure.integration.telegram.admin.service.AdminBotService;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

/**
 * Обработчик команд, связанных с заказами
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderHandler {

    private final AdminBotService adminBotService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Отправляет сообщение со всеми заказами
     */
    public SendMessage handleAllOrders(String chatId) {
        log.info("Обработка запроса на показ всех заказов для администратора {}", chatId);
        List<Order> orders = adminBotService.getOrdersByStatus(null);
        
        if (orders.isEmpty()) {
            log.info("Заказы не найдены для администратора {}", chatId);
            return createMessage(chatId, "Заказы не найдены.", AdminKeyboards.createOrderFiltersKeyboard());
        }
        
        log.info("Отправка списка всех заказов ({}) администратору {}", orders.size(), chatId);
        return createMessage(
            chatId, 
            formatOrdersList(orders, "Все заказы (" + orders.size() + "):"), 
            AdminKeyboards.createOrderFiltersKeyboard()
        );
    }
    
    /**
     * Отправляет сообщение с заказами по статусу
     */
    public SendMessage handleOrdersByStatus(String chatId, OrderStatus status) {
        log.info("Обработка запроса на показ заказов со статусом {} для администратора {}", status, chatId);
        List<Order> orders = adminBotService.getOrdersByStatus(status);
        
        if (orders.isEmpty()) {
            log.info("Заказы со статусом {} не найдены для администратора {}", status, chatId);
            return createMessage(
                chatId, 
                "Заказы со статусом " + status + " не найдены.", 
                AdminKeyboards.createOrderFiltersKeyboard()
            );
        }
        
        log.info("Отправка списка заказов со статусом {} ({}) администратору {}", status, orders.size(), chatId);
        return createMessage(
            chatId, 
            formatOrdersList(orders, "Заказы со статусом " + status + " (" + orders.size() + "):"), 
            AdminKeyboards.createOrderFiltersKeyboard()
        );
    }
    
    /**
     * Отправляет сообщение с заказами за сегодня
     */
    public SendMessage handleTodayOrders(String chatId) {
        log.info("Обработка запроса на показ заказов за сегодня для администратора {}", chatId);
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
        List<Order> allOrders = adminBotService.getOrdersByStatus(null);
        
        List<Order> todayOrders = allOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfDay))
                .toList();
        
        if (todayOrders.isEmpty()) {
            log.info("Заказы за сегодня не найдены для администратора {}", chatId);
            return createMessage(chatId, "Заказов за сегодня не найдено.", AdminKeyboards.createOrderFiltersKeyboard());
        }
        
        log.info("Отправка списка заказов за сегодня ({}) администратору {}", todayOrders.size(), chatId);
        return createMessage(
            chatId, 
            formatOrdersList(todayOrders, "Заказы за сегодня (" + todayOrders.size() + "):"), 
            AdminKeyboards.createOrderFiltersKeyboard()
        );
    }
    
    /**
     * Отправляет сообщение с заказами за неделю
     */
    public SendMessage handleWeekOrders(String chatId) {
        log.info("Обработка запроса на показ заказов за неделю для администратора {}", chatId);
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        List<Order> allOrders = adminBotService.getOrdersByStatus(null);
        
        List<Order> weekOrders = allOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfWeek))
                .toList();
        
        if (weekOrders.isEmpty()) {
            log.info("Заказы за неделю не найдены для администратора {}", chatId);
            return createMessage(chatId, "Заказов за неделю не найдено.", AdminKeyboards.createOrderFiltersKeyboard());
        }
        
        log.info("Отправка списка заказов за неделю ({}) администратору {}", weekOrders.size(), chatId);
        return createMessage(
            chatId, 
            formatOrdersList(weekOrders, "Заказы за неделю (" + weekOrders.size() + "):"), 
            AdminKeyboards.createOrderFiltersKeyboard()
        );
    }
    
    /**
     * Отправляет сообщение с заказами за месяц
     */
    public SendMessage handleMonthOrders(String chatId) {
        log.info("Обработка запроса на показ заказов за месяц для администратора {}", chatId);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<Order> allOrders = adminBotService.getOrdersByStatus(null);
        
        List<Order> monthOrders = allOrders.stream()
                .filter(order -> order.getCreatedAt().isAfter(startOfMonth))
                .toList();
        
        if (monthOrders.isEmpty()) {
            log.info("Заказы за месяц не найдены для администратора {}", chatId);
            return createMessage(chatId, "Заказов за месяц не найдено.", AdminKeyboards.createOrderFiltersKeyboard());
        }
        
        log.info("Отправка списка заказов за месяц ({}) администратору {}", monthOrders.size(), chatId);
        return createMessage(
            chatId, 
            formatOrdersList(monthOrders, "Заказы за месяц (" + monthOrders.size() + "):"), 
            AdminKeyboards.createOrderFiltersKeyboard()
        );
    }
    
    /**
     * Отправляет сообщение с детальной информацией о заказе
     */
    public SendMessage handleOrderDetails(String chatId, Long orderId) {
        log.info("Обработка запроса деталей заказа #{} от админа {}", orderId, chatId);
        
        try {
            Order order = adminBotService.getOrderById(orderId);
            
            if (order == null) {
                log.warn("Заказ #{} не найден в базе данных", orderId);
                return createMessage(chatId, "❌ Заказ #" + orderId + " не найден.", AdminKeyboards.createOrderFiltersKeyboard());
            }
            
            log.info("Заказ #{} найден, статус: {}", orderId, order.getStatus());
            
            return createMessage(chatId, formatOrderDetails(order), AdminKeyboards.createStatusKeyboard(order));
        } catch (Exception e) {
            log.error("Ошибка при получении деталей заказа #{}: {}", orderId, e.getMessage(), e);
            return createMessage(chatId, "❌ Ошибка при получении деталей заказа: " + e.getMessage(), 
                    AdminKeyboards.createBackKeyboard("filter:all"));
        }
    }
    
    /**
     * Обновляет статус заказа
     */
    public BotApiMethod<?> handleUpdateOrderStatus(String chatId, Long orderId, OrderStatus newStatus, Integer messageId) {
        Order updatedOrder = adminBotService.updateOrderStatus(orderId, newStatus);
        
        if (updatedOrder == null) {
            return createMessage(chatId, "Не удалось обновить статус заказа.");
        }
        
        if (messageId != null) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText(formatOrderDetails(updatedOrder));
            editMessage.setParseMode("Markdown");
            editMessage.setReplyMarkup(AdminKeyboards.createStatusKeyboard(updatedOrder));
            return editMessage;
        } else {
            return createMessage(
                chatId, 
                "Статус заказа #" + updatedOrder.getOrderNumber() + " обновлен на " + newStatus, 
                AdminKeyboards.createStatusKeyboard(updatedOrder)
            );
        }
    }
    
    /**
     * Отправляет сообщение со статистикой заказов
     */
    public SendMessage handleOrderStatistics(String chatId) {
        OrderStatisticsDto stats = adminBotService.getOrderStatistics();
        
        StringBuilder message = new StringBuilder("📊 *Статистика заказов*\n\n");
        
        message.append("*Общая статистика:*\n");
        message.append("📝 Всего заказов: ").append(stats.getTotalOrders()).append("\n");
        message.append("💰 Общая выручка: ").append(stats.getTotalRevenue()).append(" RUB\n");
        message.append("✅ Выполненных заказов: ").append(stats.getTotalCompletedOrders()).append("\n");
        message.append("❌ Отмененных заказов: ").append(stats.getTotalCancelledOrders()).append("\n\n");
        
        message.append("*По статусам:*\n");
        message.append("🆕 Новых: ").append(stats.getNewOrders()).append("\n");
        message.append("⏳ В обработке: ").append(stats.getProcessingOrders()).append("\n");
        message.append("📦 Отправленных: ").append(stats.getDispatchedOrders()).append("\n");
        message.append("✅ Выполненных: ").append(stats.getCompletedOrders()).append("\n");
        message.append("❌ Отмененных: ").append(stats.getCancelledOrders()).append("\n\n");
        
        message.append("*По периодам:*\n");
        message.append("📆 Сегодня: ").append(stats.getOrdersToday()).append("\n");
        message.append("📅 За неделю: ").append(stats.getOrdersThisWeek()).append("\n");
        message.append("📅 За месяц: ").append(stats.getOrdersThisMonth()).append("\n\n");
        
        message.append("*Средние показатели:*\n");
        message.append("💵 Средний чек: ").append(stats.getAverageOrderValue()).append(" RUB\n");
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createStatisticsKeyboard());
    }
    
    /**
     * Отправляет сообщение со статистикой продаж по дням
     */
    public SendMessage handleDailyStatistics(String chatId) {
        Map<LocalDate, List<Order>> ordersByDay = adminBotService.getOrdersByDays();
        
        if (ordersByDay.isEmpty()) {
            return createMessage(chatId, "Нет данных о заказах за текущий месяц.", AdminKeyboards.createBackKeyboard("stats:general"));
        }
        
        StringBuilder message = new StringBuilder("📊 *Статистика продаж по дням:*\n\n");
        
        for (Map.Entry<LocalDate, List<Order>> entry : ordersByDay.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<Order>>comparingByKey().reversed())
                .toList()) {
            
            LocalDate date = entry.getKey();
            List<Order> orders = entry.getValue();
            
            BigDecimal dailyRevenue = BigDecimal.ZERO;
            for (Order order : orders) {
                dailyRevenue = dailyRevenue.add(BigDecimal.valueOf(order.getPrice()));
            }
            
            message.append("📅 *").append(date.format(DATE_FORMATTER)).append("*\n");
            message.append("📝 Заказов: ").append(orders.size()).append("\n");
            message.append("💰 Выручка: ").append(dailyRevenue).append(" RUB\n\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createBackKeyboard("stats:general"));
    }
    
    /**
     * Отправляет сообщение с заказами пользователя
     */
    public SendMessage handleUserOrders(String chatId, Long userId) {
        User user = adminBotService.getUserById(userId);
        
        if (user == null) {
            return createMessage(chatId, "Пользователь не найден.");
        }
        
        List<Order> userOrders = adminBotService.getOrdersByUser(user);
        
        if (userOrders.isEmpty()) {
            return createMessage(
                chatId, 
                "У пользователя " + user.getUsername() + " нет заказов.", 
                AdminKeyboards.createBackKeyboard("menu:main")
            );
        }
        
        return createMessage(
            chatId, 
            formatOrdersList(userOrders, "Заказы пользователя " + user.getUsername() + " (" + userOrders.size() + "):"), 
            AdminKeyboards.createBackKeyboard("menu:main")
        );
    }
    
    /**
     * Отправляет сообщение с топом клиентов
     */
    public SendMessage handleTopUsers(String chatId) {
        List<User> topUsers = adminBotService.getTopUsersByOrderCount(10);
        
        if (topUsers.isEmpty()) {
            return createMessage(chatId, "Нет данных о пользователях.", AdminKeyboards.createBackKeyboard("stats:general"));
        }
        
        StringBuilder message = new StringBuilder("👑 *Топ клиентов по количеству заказов:*\n\n");
        
        for (int i = 0; i < topUsers.size(); i++) {
            User user = topUsers.get(i);
            List<Order> userOrders = adminBotService.getOrdersByUser(user);
            
            BigDecimal totalSpent = BigDecimal.ZERO;
            for (Order order : userOrders) {
                totalSpent = totalSpent.add(BigDecimal.valueOf(order.getPrice()));
            }
            
            message.append(i + 1).append(". *").append(user.getUsername()).append("*\n");
            message.append("📝 Заказов: ").append(userOrders.size()).append("\n");
            message.append("💰 Потрачено: ").append(totalSpent).append(" RUB\n\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createBackKeyboard("stats:general"));
    }
    
    /**
     * Отправляет инструкции по поиску заказа
     */
    public SendMessage handleOrderSearchRequest(String chatId) {
        String text = "*🔍 Поиск заказа*\n\n" +
                "Введите номер заказа, email или телефон клиента.\n\n" +
                "Примеры:\n" +
                "- `#123456` - поиск по номеру заказа\n" +
                "- `user@example.com` - поиск по email\n" +
                "- `+79001234567` - поиск по телефону";
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("filter:all"));
    }
    
    /**
     * Обрабатывает поиск заказа
     */
    public SendMessage handleOrderSearch(String chatId, String query) {
        log.info("Обработка поиска заказа с запросом: {}", query);
        query = query.trim().toLowerCase();
        List<Order> orders = new ArrayList<>();

        if (query.startsWith("#")) {
            String orderNumber = query.substring(1);
            orders = adminBotService.searchOrdersByOrderNumber(orderNumber);
        } else if (query.contains("@")) {
            // Поиск по email
            orders = adminBotService.searchOrdersByEmail(query);
        } else if (query.startsWith("+")) {
            // Поиск по телефону
            orders = adminBotService.searchOrdersByPhone(query);
        } else {
            try {
                long orderId = Long.parseLong(query);
                Order order = adminBotService.getOrderById(orderId);
                if (order != null) {
                    orders = List.of(order);
                }
            } catch (NumberFormatException e) {
                // Если не распознали как число, возвращаем пустой список
                // Метода getOrdersByCustomerName нет в AdminBotService
                log.info("Не удалось преобразовать запрос в orderId: {}", query);
            }
        }

        if (orders.isEmpty()) {
            return createMessage(chatId, "⚠️ Заказы не найдены", AdminKeyboards.createBackKeyboard("filter:all"));
        }

        String title = "🔍 Найдено заказов: " + orders.size();
        String formattedOrders = formatOrdersList(orders, title);

        InlineKeyboardMarkup keyboard = AdminKeyboards.createBackKeyboard("filter:all");
        return createMessage(chatId, formattedOrders, keyboard);
    }
    
    /**
     * Форматирует список заказов
     */
    private String formatOrdersList(List<Order> orders, String title) {
        StringBuilder message = new StringBuilder("*" + title + "*\n\n");
        
        for (Order order : orders) {
            message.append("🔹 ").append(getStatusEmoji(order.getStatus()))
                  .append(" #").append(order.getOrderNumber());
            
            if (order.getCreatedAt() != null) {
                message.append(" (").append(order.getCreatedAt().format(DATETIME_FORMATTER)).append(")");
            }
            
            message.append("\n");
            message.append("📦 Товар: ").append(escapeMarkdown(order.getProduct().getName()))
                  .append(", Размер: ").append(order.getSize())
                  .append(", Цена: ").append(order.getPrice()).append(" RUB\n");
            message.append("👤 Клиент: ").append(escapeMarkdown(order.getUser().getUsername())).append("\n");
            message.append("/order\\_").append(order.getId()).append(" - подробнее\n\n");
        }
        
        return message.toString();
    }
    
    /**
     * Форматирует детальную информацию о заказе
     */
    private String formatOrderDetails(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        
        StringBuilder message = new StringBuilder();
        message.append("*Заказ #").append(order.getOrderNumber()).append("*\n\n");
        
        message.append("*Статус:* ").append(getStatusEmoji(order.getStatus())).append(" ").append(order.getStatus()).append("\n");
        message.append("*ID заказа:* ").append(order.getId()).append("\n");
        message.append("*Продукт:* ").append(escapeMarkdown(order.getProduct().getName())).append("\n");
        message.append("*Размер:* ").append(order.getSize()).append("\n");
        message.append("*Количество:* ").append(order.getQuantity()).append("\n");
        message.append("*Цена:* ").append(order.getPrice()).append(" RUB\n\n");
        
        message.append("*Контакты:*\n");
        message.append("   *Email:* ").append(escapeMarkdown(order.getEmail())).append("\n");
        message.append("   *Телефон:* ").append(escapeMarkdown(order.getPhoneNumber())).append("\n\n");
        
        message.append("*Доставка:*\n");
        message.append("   *Полное имя:* ").append(escapeMarkdown(order.getFullName())).append("\n");
        message.append("   *Страна:* ").append(escapeMarkdown(order.getCountry())).append("\n");
        message.append("   *Адрес:* ").append(escapeMarkdown(order.getAddress())).append("\n");
        message.append("   *Почтовый индекс:* ").append(escapeMarkdown(order.getPostalCode())).append("\n\n");
        
        message.append("*Дополнительно:*\n");
        message.append("   *Telegram:* ").append(order.getTelegramUsername() != null ? escapeMarkdown(order.getTelegramUsername()) : "-").append("\n");
        message.append("   *Crypto адрес:* ").append(order.getCryptoAddress() != null ? escapeMarkdown(order.getCryptoAddress()) : "-").append("\n");
        message.append("   *Комментарий:* ").append(order.getOrderComment() != null ? escapeMarkdown(order.getOrderComment()) : "-").append("\n");
        message.append("   *Промо код:* ").append(order.getPromoCode() != null ? escapeMarkdown(order.getPromoCode()) : "-").append("\n");
        message.append("   *Способ оплаты:* ").append(escapeMarkdown(order.getPaymentMethod())).append("\n\n");
        
        message.append("*Создан:* ").append(order.getCreatedAt().format(formatter)).append("\n");
        if (order.getUpdatedAt() != null) {
            message.append("*Обновлен:* ").append(order.getUpdatedAt().format(formatter)).append("\n");
        }
        
        return message.toString();
    }
    
    /**
     * Создаёт объект сообщения
     */
    private SendMessage createMessage(String chatId, String text) {
        log.debug("Создание простого сообщения для администратора {}", chatId);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }
    
    /**
     * Создаёт объект сообщения с клавиатурой
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        log.debug("Создание сообщения с клавиатурой для администратора {}", chatId);
        SendMessage message = createMessage(chatId, text);
        message.setReplyMarkup(keyboard);
        
        // Логируем структуру клавиатуры
        if (keyboard != null && keyboard.getKeyboard() != null) {
            StringBuilder keyboardInfo = new StringBuilder("Клавиатура: ");
            for (List<InlineKeyboardButton> row : keyboard.getKeyboard()) {
                for (InlineKeyboardButton button : row) {
                    keyboardInfo.append("[")
                            .append(button.getText())
                            .append(" -> ")
                            .append(button.getCallbackData())
                            .append("] ");
                }
                keyboardInfo.append("\n");
            }
            log.debug(keyboardInfo.toString());
        }
        
        return message;
    }
    
    /**
     * Возвращает эмодзи для статуса заказа
     */
    private String getStatusEmoji(OrderStatus status) {
        return switch (status) {
            case NEW -> "🆕";
            case PROCESSING -> "⏳";
            case DISPATCHED -> "📦";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
    }
    
    /**
     * Экранирует Markdown символы в тексте
     */
    private String escapeMarkdown(String text) {
        return text.replace("*", "\\*")
                   .replace("_", "\\_")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace("(", "\\(")
                   .replace(")", "\\)");
    }
} 