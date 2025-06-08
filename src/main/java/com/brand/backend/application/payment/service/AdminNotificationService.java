package com.brand.backend.application.payment.service;

import com.brand.backend.domain.balance.model.Transaction;
import com.brand.backend.infrastructure.config.PaymentProperties;
import com.brand.backend.infrastructure.integration.telegram.admin.AdminTelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Сервис для отправки уведомлений администраторам
 */
@Service
@Slf4j
public class AdminNotificationService {
    
    private final PaymentProperties paymentProperties;
    private final ApplicationContext applicationContext;
    
    public AdminNotificationService(PaymentProperties paymentProperties, ApplicationContext applicationContext) {
        this.paymentProperties = paymentProperties;
        this.applicationContext = applicationContext;
    }
    
    /**
     * Уведомить администраторов о новом запросе на пополнение баланса
     */
    public void notifyNewDepositRequest(Transaction transaction) {
        log.info("Отправка уведомления администраторам о новом запросе на пополнение: {}", transaction.getTransactionCode());
        
        String message = formatDepositNotification(transaction);
        
        // Создаем клавиатуру с кнопками для подтверждения/отклонения
        InlineKeyboardMarkup keyboard = createDepositActionKeyboard(transaction.getId());
        
        try {
            // Получаем AdminTelegramBot из ApplicationContext только когда он нужен
            AdminTelegramBot adminTelegramBot = applicationContext.getBean(AdminTelegramBot.class);
            
            // Получаем список разрешенных админских чатов из Telegram-бота
            List<Long> adminIds = adminTelegramBot.getAllowedAdminIds();
            for (Long chatId : adminIds) {
                sendMessageWithKeyboard(adminTelegramBot, chatId.toString(), message, keyboard);
            }
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления админам: {}", e.getMessage(), e);
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
     * Создает клавиатуру для действий с пополнением
     */
    private InlineKeyboardMarkup createDepositActionKeyboard(Long transactionId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Ряд кнопок для действий
        List<InlineKeyboardButton> row = new ArrayList<>();
        
        // Кнопка подтверждения
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("✅ Подтвердить");
        confirmButton.setCallbackData("deposit_confirm_" + transactionId);
        row.add(confirmButton);
        
        // Кнопка отклонения
        InlineKeyboardButton rejectButton = new InlineKeyboardButton();
        rejectButton.setText("❌ Отклонить");
        rejectButton.setCallbackData("deposit_reject_" + transactionId);
        row.add(rejectButton);
        
        keyboard.add(row);
        
        // Второй ряд для просмотра деталей
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        
        // Кнопка деталей
        InlineKeyboardButton detailsButton = new InlineKeyboardButton();
        detailsButton.setText("📋 Подробности");
        detailsButton.setCallbackData("deposit_details_" + transactionId);
        row2.add(detailsButton);
        
        keyboard.add(row2);
        
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
    
    /**
     * Форматировать сообщение о пополнении баланса
     */
    private String formatDepositNotification(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        
        return String.format(
                "<b>🔔 НОВОЕ ПОПОЛНЕНИЕ БАЛАНСА</b>\n\n" +
                "<b>Пользователь:</b> %s\n" +
                "<b>Сумма:</b> %.2f ₽\n" +
                "<b>Код транзакции:</b> <code>%s</code>\n\n" +
                "❗️ <i>Пользователь должен перевести %.2f ₽ на карту %s с комментарием <code>%s</code></i>\n\n" +
                "Используйте кнопки ниже для управления транзакцией",
                transaction.getUser().getUsername(),
                amount,
                transaction.getTransactionCode(),
                amount,
                paymentProperties.getCardNumber(),
                transaction.getTransactionCode()
        );
    }
} 