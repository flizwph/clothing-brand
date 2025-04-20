package com.brand.backend.application.nft.handler;

import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.domain.nft.event.NFTEvent;
import com.brand.backend.domain.nft.model.NFT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик событий, связанных с NFT
 */
@Component
public class NFTEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NFTEventHandler.class);
    
    private final TelegramBotService telegramBotService;
    
    @org.springframework.beans.factory.annotation.Autowired
    public NFTEventHandler(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @Async("eventExecutor")
    @EventListener
    public void handleNFTEvent(NFTEvent event) {
        NFT nft = event.getNft();
        log.debug("Обработка события NFT: {}, тип: {}", nft.getId(), event.getEventType());
        
        try {
            switch (event.getEventType()) {
                case CREATED:
                    notifyUserNFTCreated(nft);
                    break;
                    
                case REVEALED:
                    notifyUserNFTRevealed(nft);
                    break;
                    
                case TRANSFERRED:
                    notifyUserNFTTransferred(nft);
                    break;
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события NFT {}: {}", nft.getId(), e.getMessage(), e);
        }
    }
    
    private void notifyUserNFTCreated(NFT nft) {
        if (nft.getUser().getTelegramId() != null) {
            String message = "🎁 Вам выдан новый NFT!\n" +
                    "Это placeholder-версия, которая будет раскрыта после доставки заказа.";
            
            SendMessage sendMessage = createNFTMessage(
                    nft.getUser().getTelegramId().toString(), 
                    message,
                    nft.getPlaceholderUri()
            );
            
            try {
                telegramBotService.execute(sendMessage);
                log.info("Уведомление о создании NFT отправлено пользователю: {}", nft.getUser().getUsername());
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки уведомления о создании NFT: {}", e.getMessage());
            }
        }
    }
    
    private void notifyUserNFTRevealed(NFT nft) {
        if (nft.getUser().getTelegramId() != null) {
            String message = "✨ Ваш NFT раскрыт!\n" +
                    "Редкость: " + nft.getRarity();
            
            SendMessage sendMessage = createNFTMessage(
                    nft.getUser().getTelegramId().toString(), 
                    message,
                    nft.getRevealedUri()
            );
            
            try {
                telegramBotService.execute(sendMessage);
                log.info("Уведомление о раскрытии NFT отправлено пользователю: {}", nft.getUser().getUsername());
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки уведомления о раскрытии NFT: {}", e.getMessage());
            }
        }
    }
    
    private void notifyUserNFTTransferred(NFT nft) {
        if (nft.getUser().getTelegramId() != null) {
            String message = "🔄 NFT успешно передан на ваш кошелек.";
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(nft.getUser().getTelegramId().toString());
            sendMessage.setText(message);
            
            try {
                telegramBotService.execute(sendMessage);
                log.info("Уведомление о передаче NFT отправлено пользователю: {}", nft.getUser().getUsername());
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки уведомления о передаче NFT: {}", e.getMessage());
            }
        }
    }
    
    private SendMessage createNFTMessage(String chatId, String text, String imageUrl) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text + "\n\nИзображение: " + imageUrl);
        
        // Создаем кнопки для просмотра NFT
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        
        InlineKeyboardButton viewButton = new InlineKeyboardButton();
        viewButton.setText("Просмотреть NFT");
        viewButton.setUrl(imageUrl);
        row.add(viewButton);
        
        rows.add(row);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        
        return message;
    }
} 