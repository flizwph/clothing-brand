package com.brand.backend.application.order.service;

import com.brand.backend.domain.order.model.DigitalOrder;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.infrastructure.integration.telegram.admin.AdminTelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для отправки уведомлений администраторам о заказах
 */
@Service
@Slf4j
public class OrderNotificationService {
    
    private final ApplicationContext applicationContext;
    
    public OrderNotificationService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Уведомить администраторов о новом заказе
     */
    public void notifyNewOrder(Order order) {
        log.info("Отправка уведомления администраторам о новом заказе: {}", order.getOrderNumber());
        
        String message = formatOrderNotification(order);
        
        // Создаем клавиатуру с кнопками для управления заказом
        InlineKeyboardMarkup keyboard = createOrderActionKeyboard(order.getId());
        
        try {
            // Получаем AdminTelegramBot из ApplicationContext только когда он нужен
            AdminTelegramBot adminTelegramBot = applicationContext.getBean(AdminTelegramBot.class);
            
            // Получаем список разрешенных админских чатов из Telegram-бота
            Set<String> adminIds = adminTelegramBot.getAllowedAdminIds();
            for (String chatId : adminIds) {
                sendMessageWithKeyboard(adminTelegramBot, chatId, message, keyboard);
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления админам о заказе: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Уведомить администраторов о новом цифровом заказе
     */
    public void notifyNewDigitalOrder(DigitalOrder order) {
        log.info("Отправка уведомления администраторам о новом цифровом заказе: {}", order.getOrderNumber());
        
        String message = formatDigitalOrderNotification(order);
        
        // Создаем клавиатуру с кнопками для управления заказом
        InlineKeyboardMarkup keyboard = createDigitalOrderActionKeyboard(order.getId());
        
        try {
            // Получаем AdminTelegramBot из ApplicationContext только когда он нужен
            AdminTelegramBot adminTelegramBot = applicationContext.getBean(AdminTelegramBot.class);
            
            // Получаем список разрешенных админских чатов из Telegram-бота
            Set<String> adminIds = adminTelegramBot.getAllowedAdminIds();
            for (String chatId : adminIds) {
                sendMessageWithKeyboard(adminTelegramBot, chatId, message, keyboard);
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления админам о цифровом заказе: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Отправляет сообщение с клавиатурой
     */
    private void sendMessageWithKeyboard(AdminTelegramBot bot, String chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboard);
        
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в Telegram админу {}: {}", chatId, e.getMessage(), e);
        }
    }
    
    /**
     * Создает клавиатуру для действий с заказом
     */
    private InlineKeyboardMarkup createOrderActionKeyboard(Long orderId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Ряд кнопок для изменения статуса
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        
        // Кнопка "В обработке"
        InlineKeyboardButton processingButton = new InlineKeyboardButton();
        processingButton.setText("⏳ В обработке");
        processingButton.setCallbackData("updateOrder:" + orderId + ":PROCESSING");
        row1.add(processingButton);
        
        // Кнопка "Отправлен"
        InlineKeyboardButton dispatchedButton = new InlineKeyboardButton();
        dispatchedButton.setText("📦 Отправлен");
        dispatchedButton.setCallbackData("updateOrder:" + orderId + ":DISPATCHED");
        row1.add(dispatchedButton);
        
        keyboard.add(row1);
        
        // Второй ряд для просмотра деталей
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        
        // Кнопка деталей заказа
        InlineKeyboardButton detailsButton = new InlineKeyboardButton();
        detailsButton.setText("📋 Подробности");
        detailsButton.setCallbackData("viewOrder:" + orderId);
        row2.add(detailsButton);
        
        // Кнопка деталей клиента
        InlineKeyboardButton userButton = new InlineKeyboardButton();
        userButton.setText("👤 Информация о клиенте");
        userButton.setCallbackData("viewUser:" + orderId + ":fromOrder");
        row2.add(userButton);
        
        keyboard.add(row2);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
    
    /**
     * Создает клавиатуру для действий с цифровым заказом
     */
    private InlineKeyboardMarkup createDigitalOrderActionKeyboard(Long orderId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Ряд кнопок для основных действий
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        
        // Кнопка "Подтвердить оплату"
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("✅ Подтвердить оплату");
        confirmButton.setCallbackData("confirmDigitalOrder:" + orderId);
        row1.add(confirmButton);
        
        keyboard.add(row1);
        
        // Второй ряд для просмотра деталей
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        
        // Кнопка деталей заказа
        InlineKeyboardButton detailsButton = new InlineKeyboardButton();
        detailsButton.setText("📋 Подробности");
        detailsButton.setCallbackData("viewDigitalOrder:" + orderId);
        row2.add(detailsButton);
        
        // Кнопка деталей клиента
        InlineKeyboardButton userButton = new InlineKeyboardButton();
        userButton.setText("👤 Информация о клиенте");
        userButton.setCallbackData("viewUserDigital:" + orderId);
        row2.add(userButton);
        
        keyboard.add(row2);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
    
    /**
     * Форматировать сообщение о новом заказе
     */
    private String formatOrderNotification(Order order) {
        StringBuilder addressInfo = new StringBuilder();
        if (order.getAddress() != null && !order.getAddress().isEmpty()) {
            addressInfo.append("<b>Адрес:</b> ").append(order.getCountry() != null ? order.getCountry() + ", " : "")
                    .append(order.getAddress());
            
            if (order.getPostalCode() != null && !order.getPostalCode().isEmpty()) {
                addressInfo.append(", ").append(order.getPostalCode());
            }
            addressInfo.append("\n");
        }
        
        return String.format(
                "<b>🔔 НОВЫЙ ЗАКАЗ #%s</b>\n\n" +
                "<b>Клиент:</b> %s\n" +
                "<b>Товар:</b> %s\n" +
                "<b>Размер:</b> %s\n" +
                "<b>Количество:</b> %d\n" +
                "<b>Сумма:</b> %.2f ₽\n" +
                "<b>Способ оплаты:</b> %s\n" +
                "%s" +
                "<b>Email:</b> %s\n" +
                "<b>Телефон:</b> %s\n" +
                "%s" +
                "<b>Дата создания:</b> %s\n\n" +
                "Используйте кнопки ниже для управления заказом",
                order.getOrderNumber(),
                order.getUser().getUsername(),
                order.getProduct().getName(),
                order.getSize(),
                order.getQuantity(),
                order.getPrice(),
                order.getPaymentMethod(),
                addressInfo.toString(),
                order.getEmail() != null ? order.getEmail() : "Не указан",
                order.getPhoneNumber() != null ? order.getPhoneNumber() : "Не указан",
                order.getOrderComment() != null && !order.getOrderComment().isEmpty() ? 
                        "<b>Комментарий:</b> " + order.getOrderComment() + "\n" : "",
                order.getCreatedAt().toString().replace("T", " ").substring(0, 16)
        );
    }
    
    /**
     * Форматировать сообщение о новом цифровом заказе
     */
    private String formatDigitalOrderNotification(DigitalOrder order) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
        
        // Форматируем список товаров
        String productsInfo = order.getItems().stream()
                .map(item -> String.format("• %s (количество: %d, цена: %s)\n", 
                        item.getDigitalProduct().getName(), 
                        item.getQuantity(),
                        formatter.format(item.getPrice())))
                .collect(Collectors.joining());
        
        return String.format(
                "<b>🔔 НОВЫЙ ЦИФРОВОЙ ЗАКАЗ #%s</b>\n\n" +
                "<b>Клиент:</b> %s\n" +
                "<b>Товары:</b>\n%s" +
                "<b>Общая сумма:</b> %s\n" +
                "<b>Способ оплаты:</b> %s\n" +
                "<b>Статус оплаты:</b> %s\n" +
                "<b>Email:</b> %s\n" +
                "<b>Дата создания:</b> %s\n\n" +
                "Используйте кнопки ниже для управления заказом",
                order.getOrderNumber(),
                order.getUser().getUsername(),
                productsInfo,
                formatter.format(order.getTotalPrice()),
                order.getPaymentMethod(),
                order.isPaid() ? "✅ Оплачен" : "❌ Не оплачен",
                order.getUser().getEmail() != null ? order.getUser().getEmail() : "Не указан",
                order.getCreatedAt().toString().replace("T", " ").substring(0, 16)
        );
    }
}