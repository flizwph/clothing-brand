package com.brand.backend.application.order.handler;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.infrastructure.integration.telegram.admin.AdminTelegramBot;
import com.brand.backend.domain.order.event.OrderEvent;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.order.model.Order;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.application.nft.service.NFTService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Обработчик событий, связанных с заказами
 */
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    private final TelegramBotService telegramBotService;
    private final AdminTelegramBot adminTelegramBot;
    private final NFTService nftService;

    @Async("eventExecutor")
    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        Order order = event.getOrder();
        log.debug("Обработка события заказа: {}, тип: {}", order.getOrderNumber(), event.getEventType());
        
        try {
            switch (event.getEventType()) {
                case CREATED:
                    notifyUserOrderCreated(order);
                    notifyAdminOrderCreated(order);
                    break;
                    
                case PAID:
                    createNFTForOrder(order);
                    notifyUserOrderPaid(order);
                    break;
                    
                case UPDATED:
                    notifyUserOrderUpdated(order);
                    break;
                    
                case SHIPPED:
                    notifyUserOrderShipped(order);
                    break;
                    
                case DELIVERED:
                    notifyUserOrderDelivered(order);
                    break;
                    
                case CANCELED:
                    notifyUserOrderCanceled(order);
                    notifyAdminOrderCanceled(order);
                    break;
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события заказа {}: {}", order.getOrderNumber(), e.getMessage(), e);
        }
    }
    
    private void createNFTForOrder(Order order) {
        if (order.getStatus() == OrderStatus.PROCESSING) {
            try {
                String placeholderUri = "https://brand.com/nft/placeholder";
                String rarity = "common"; // Базовая редкость, может быть изменена в будущем
                NFT nft = nftService.createNFTForOrder(order, placeholderUri, rarity);
                log.info("NFT создан для заказа: {}, NFT ID: {}", order.getOrderNumber(), nft.getId());
            } catch (Exception e) {
                log.error("Ошибка при создании NFT для заказа: {}", order.getOrderNumber(), e);
            }
        }
    }
    
    private void notifyUserOrderCreated(Order order) {
        if (order.getUser().getTelegramId() != null) {
            sendTelegramMessage(
                order.getUser().getTelegramId().toString(),
                "✅ Заказ #" + order.getOrderNumber() + " успешно создан!\n" +
                "Товар: " + order.getProduct().getName() + "\n" +
                "Размер: " + order.getSize() + "\n" +
                "Сумма: " + order.getPrice() + " RUB"
            );
        }
    }
    
    private void notifyAdminOrderCreated(Order order) {
        sendAdminTelegramMessage(
            "🔔 НОВЫЙ ЗАКАЗ #" + order.getOrderNumber() + "\n" +
            "Товар: " + order.getProduct().getName() + "\n" +
            "Размер: " + order.getSize() + "\n" +
            "Сумма: " + order.getPrice() + " RUB\n" +
            "Покупатель: " + order.getUser().getUsername()
        );
    }
    
    private void notifyUserOrderPaid(Order order) {
        if (order.getUser().getTelegramId() != null) {
            sendTelegramMessage(
                order.getUser().getTelegramId().toString(),
                "💰 Заказ #" + order.getOrderNumber() + " успешно оплачен!\n" +
                "Вам начислен NFT, который станет доступен после выполнения заказа."
            );
        }
    }
    
    private void notifyUserOrderUpdated(Order order) {
        if (order.getUser().getTelegramId() != null) {
            sendTelegramMessage(
                order.getUser().getTelegramId().toString(),
                "🔄 Статус заказа #" + order.getOrderNumber() + " обновлен: " + order.getStatus()
            );
        }
    }
    
    private void notifyUserOrderShipped(Order order) {
        if (order.getUser().getTelegramId() != null) {
            sendTelegramMessage(
                order.getUser().getTelegramId().toString(),
                "🚚 Заказ #" + order.getOrderNumber() + " отправлен!\n" +
                "Скоро он будет у вас."
            );
        }
    }
    
    private void notifyUserOrderDelivered(Order order) {
        if (order.getUser().getTelegramId() != null) {
            sendTelegramMessage(
                order.getUser().getTelegramId().toString(),
                "📦 Заказ #" + order.getOrderNumber() + " доставлен!\n" +
                "Спасибо за покупку в нашем магазине."
            );
        }
    }
    
    private void notifyUserOrderCanceled(Order order) {
        if (order.getUser().getTelegramId() != null) {
            sendTelegramMessage(
                order.getUser().getTelegramId().toString(),
                "❌ Заказ #" + order.getOrderNumber() + " отменен."
            );
        }
    }
    
    private void notifyAdminOrderCanceled(Order order) {
        sendAdminTelegramMessage(
            "❌ ЗАКАЗ ОТМЕНЕН #" + order.getOrderNumber() + "\n" +
            "Пользователь: " + order.getUser().getUsername()
        );
    }
    
    private void sendTelegramMessage(String chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            telegramBotService.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в Telegram: {}", e.getMessage());
        }
    }
    
    private void sendAdminTelegramMessage(String text) {
        try {
            for (String chatId : adminTelegramBot.getAllowedAdminIds()) {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(text);
                adminTelegramBot.execute(message);
                log.debug("Отправлено сообщение администратору с ID {}", chatId);
            }
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения администратору: {}", e.getMessage());
        }
    }
} 