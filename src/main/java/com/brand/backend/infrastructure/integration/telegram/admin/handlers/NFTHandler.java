package com.brand.backend.infrastructure.integration.telegram.admin.handlers;

import com.brand.backend.infrastructure.integration.telegram.admin.keyboards.AdminKeyboards;
import com.brand.backend.infrastructure.integration.telegram.admin.service.AdminBotService;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик команд, связанных с NFT
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NFTHandler {

    private final AdminBotService adminBotService;

    /**
     * Обрабатывает различные команды для NFT
     */
    public void handleNftCallback(String chatId, String command) {
        log.debug("Обработка NFT callback для команды: {}", command);
        
        switch (command) {
            case "all" -> handleAllNFTs(chatId);
            case "unrevealed" -> handleUnrevealedNFTs(chatId);
            case "searchByUser" -> handleNFTSearchForm(chatId);
            default -> handleNFTMenu(chatId);
        }
    }
    
    /**
     * Отправляет список всех NFT
     */
    public SendMessage handleAllNFTs(String chatId) {
        log.debug("Запрос на получение списка всех NFT от {}", chatId);
        List<NFT> allNFTs = adminBotService.getAllNFTs();
        
        if (allNFTs.isEmpty()) {
            return createMessage(chatId, "NFT не найдены.", AdminKeyboards.createBackKeyboard("nft:menu"));
        }
        
        return createNFTListMessage(chatId, allNFTs, "Все NFT (" + allNFTs.size() + "):");
    }
    
    /**
     * Отправляет список нераскрытых NFT
     */
    public SendMessage handleUnrevealedNFTs(String chatId) {
        log.debug("Запрос на получение списка нераскрытых NFT от {}", chatId);
        List<NFT> unrevealedNFTs = adminBotService.getUnrevealedNFTs();
        
        if (unrevealedNFTs.isEmpty()) {
            return createMessage(chatId, "Нераскрытые NFT не найдены.", AdminKeyboards.createBackKeyboard("nft:menu"));
        }
        
        return createNFTListMessage(chatId, unrevealedNFTs, "Нераскрытые NFT (" + unrevealedNFTs.size() + "):");
    }
    
    /**
     * Обрабатывает запрос на поиск NFT по владельцу
     */
    public SendMessage handleNFTSearchForm(String chatId) {
        log.debug("Запрос на форму поиска NFT от {}", chatId);
        String text = """
                *🔍 Поиск NFT по владельцу*
                
                Введите имя пользователя или email для поиска NFT.
                
                Например:
                /nft_search username
                /nft_search email@example.com
                """;
        
        return createMessage(chatId, text, AdminKeyboards.createBackKeyboard("nft:menu"));
    }
    
    /**
     * Отправляет меню NFT
     */
    public SendMessage handleNFTMenu(String chatId) {
        log.debug("Запрос на меню NFT от {}", chatId);
        String text = "*🎨 Управление NFT*\n\nВыберите опцию:";
        return createMessage(chatId, text, AdminKeyboards.createNFTKeyboard());
    }
    
    /**
     * Обрабатывает поиск NFT по владельцу
     */
    public SendMessage handleNFTSearch(String chatId, String query) {
        log.debug("Поиск NFT по запросу '{}' для {}", query, chatId);
        User user = adminBotService.getUserByUsername(query);
        
        if (user == null) {
            return createMessage(chatId, "Пользователь по запросу '" + query + "' не найден.", 
                    AdminKeyboards.createBackKeyboard("nft:menu"));
        }
        
        List<NFT> userNFTs = adminBotService.getNFTsByUser(user);
        
        if (userNFTs.isEmpty()) {
            return createMessage(chatId, "У пользователя " + user.getUsername() + " нет NFT.", 
                    AdminKeyboards.createBackKeyboard("nft:menu"));
        }
        
        return createNFTListMessage(chatId, userNFTs, "NFT пользователя " + user.getUsername() + " (" + userNFTs.size() + "):");
    }
    
    /**
     * Отправляет список всех NFT
     */
    public SendMessage handleNftList(String chatId) {
        return handleAllNFTs(chatId);
    }
    
    /**
     * Создаёт сообщение со списком NFT
     */
    private SendMessage createNFTListMessage(String chatId, List<NFT> nfts, String title) {
        StringBuilder message = new StringBuilder("*" + title + "*\n\n");
        
        for (int i = 0; i < nfts.size(); i++) {
            NFT nft = nfts.get(i);
            message.append(i + 1).append(". ID: ").append(nft.getId()).append("\n");
            message.append("   Placeholder URI: ").append(nft.getPlaceholderUri()).append("\n");
            message.append("   Раскрыт: ").append(nft.isRevealed() ? "✅" : "❌").append("\n");
            
            if (nft.isRevealed() && nft.getRevealedUri() != null) {
                message.append("   Revealed URI: ").append(nft.getRevealedUri()).append("\n");
            }
            
            if (nft.getRarity() != null) {
                message.append("   Редкость: ").append(nft.getRarity()).append("\n");
            }
            
            User owner = nft.getUser();
            if (owner != null) {
                message.append("   Владелец: ").append(owner.getUsername()).append("\n");
            }
            
            message.append("\n");
        }
        
        return createMessage(chatId, message.toString(), AdminKeyboards.createBackKeyboard("nft:menu"));
    }
    
    /**
     * Создаёт объект сообщения
     */
    private SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }
    
    /**
     * Создаёт объект сообщения с клавиатурой
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        SendMessage message = createMessage(chatId, text);
        message.setReplyMarkup(keyboard);
        return message;
    }
    
    /**
     * Создаёт кнопку с текстом и callback-данными
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
    
    /**
     * Обрабатывает запрос на просмотр NFT пользователя
     */
    public SendMessage handleUserNFTs(String chatId, Long userId) {
        log.info(">> Обработка запроса на просмотр NFT пользователя: {}", userId);
        try {
            User user = adminBotService.getUserById(userId);
            
            if (user == null) {
                log.warn("Пользователь с ID {} не найден", userId);
                return createMessage(chatId, "❌ Пользователь не найден.");
            }
            
            List<NFT> userNFTs = adminBotService.getNFTsByUser(user);
            
            if (userNFTs.isEmpty()) {
                log.info("У пользователя {} нет NFT", user.getUsername());
                return createMessage(
                    chatId, 
                    "У пользователя " + user.getUsername() + " нет NFT.", 
                    AdminKeyboards.createBackKeyboard("viewUser:" + userId)
                );
            }
            
            StringBuilder message = new StringBuilder();
            message.append("🎨 *NFT пользователя ").append(user.getUsername()).append("*\n\n");
            
            for (int i = 0; i < userNFTs.size(); i++) {
                NFT nft = userNFTs.get(i);
                message.append(i + 1).append(". ID: ").append(nft.getId()).append("\n");
                message.append("   Placeholder URI: ").append(nft.getPlaceholderUri()).append("\n");
                message.append("   Раскрыт: ").append(nft.isRevealed() ? "✅" : "❌").append("\n");
                if (nft.isRevealed() && nft.getRevealedUri() != null) {
                    message.append("   Revealed URI: ").append(nft.getRevealedUri()).append("\n");
                }
                if (nft.getRarity() != null) {
                    message.append("   Редкость: ").append(nft.getRarity()).append("\n");
                }
                message.append("\n");
            }
            
            return createMessage(
                chatId, 
                message.toString(), 
                AdminKeyboards.createBackKeyboard("viewUser:" + userId)
            );
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на просмотр NFT пользователя: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        }
    }
} 