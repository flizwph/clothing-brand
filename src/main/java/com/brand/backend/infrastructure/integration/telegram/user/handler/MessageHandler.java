package com.brand.backend.infrastructure.integration.telegram.user.handler;

import com.brand.backend.infrastructure.integration.telegram.user.command.Command;
import com.brand.backend.infrastructure.integration.telegram.user.command.CommandFactory;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.infrastructure.integration.telegram.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Обработчик сообщений от пользователя
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageHandler {

    private final CommandFactory commandFactory;
    private final UserSessionService userSessionService;
    
    /**
     * Обрабатывает входящее сообщение и возвращает ответ для выполнения
     * 
     * @param message сообщение пользователя
     * @return ответное сообщение или null
     */
    public BotApiMethod<?> handle(Message message) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText();
        
        if (text == null) {
            return createSendMessage(chatId, "Я понимаю только текстовые сообщения.");
        }
        
        log.info("Получено сообщение от пользователя {}: {}", chatId, text);
        
        TelegramBotService dummyBot = new TelegramBotServiceAdapter();
        
        // Проверяем, находится ли пользователь в особом состоянии
        String state = userSessionService.getUserState(String.valueOf(message.getChatId()));
        if (state != null) {
            handleStateBasedMessage(message, state, dummyBot);
            return null;
        }
        
        // Обрабатываем команды
        if (text.startsWith("/")) {
            Command command = commandFactory.getCommand(text);
            if (command != null) {
                log.info("Выполняется команда: {}", text);
                command.execute(message, dummyBot);
                return null;
            } else {
                log.warn("Неизвестная команда: {}", text);
                return createSendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
            }
        }
        
        // Обрабатываем обычный текст
        log.info("Обычное сообщение от пользователя {}: {}", chatId, text);
        return createSendMessage(chatId, "Я не понимаю ваше сообщение. Используйте /help для просмотра доступных команд.");
    }
    
    /**
     * Создает объект сообщения для отправки
     * 
     * @param chatId ID чата
     * @param text текст сообщения
     * @return объект сообщения
     */
    private SendMessage createSendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }
    
    /**
     * Обрабатывает входящее сообщение
     * 
     * @param message сообщение пользователя
     * @param bot экземпляр бота
     */
    public void handleMessage(Message message, TelegramBotService bot) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText();
        
        if (text == null) {
            bot.sendMessage(chatId, "Я понимаю только текстовые сообщения.");
            return;
        }
        
        log.info("Получено сообщение от пользователя {}: {}", chatId, text);
        
        // Проверяем, находится ли пользователь в особом состоянии (например, ожидание ввода данных)
        String state = userSessionService.getUserState(String.valueOf(message.getChatId()));
        if (state != null) {
            handleStateBasedMessage(message, state, bot);
            return;
        }
        
        // Обрабатываем команды
        if (text.startsWith("/")) {
            handleCommand(message, bot);
            return;
        }
        
        // Обрабатываем обычный текст
        handleTextMessage(message, bot);
    }
    
    /**
     * Обрабатывает сообщение в зависимости от состояния пользователя
     * 
     * @param message сообщение пользователя
     * @param state состояние пользователя
     * @param bot экземпляр бота
     */
    private void handleStateBasedMessage(Message message, String state, TelegramBotService bot) {
        Long chatId = message.getChatId();
        String text = message.getText();
        
        switch (state) {
            case "linkTelegram":
                handleLinkTelegramState(message, bot);
                break;
            case "linkDiscord":
                handleLinkDiscordState(message, bot);
                break;
            case "searchOrder":
                handleSearchOrderState(message, bot);
                break;
            default:
                log.warn("Неизвестное состояние пользователя {}: {}", chatId, state);
                userSessionService.clearSession(String.valueOf(chatId));
                bot.sendMessage(String.valueOf(chatId), "Произошла ошибка. Пожалуйста, начните снова.");
        }
    }
    
    /**
     * Обрабатывает команду
     * 
     * @param message сообщение пользователя
     * @param bot экземпляр бота
     */
    private void handleCommand(Message message, TelegramBotService bot) {
        String commandText = message.getText();
        String chatId = String.valueOf(message.getChatId());
        
        // Обрабатываем команду через фабрику команд
        Command command = commandFactory.getCommand(commandText);
        if (command != null) {
            log.info("Выполняется команда: {}", commandText);
            command.execute(message, bot);
            return;
        }
        
        // Если команда не найдена, отправляем сообщение о неизвестной команде
        log.warn("Неизвестная команда: {}", commandText);
        bot.sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
    }
    
    /**
     * Обрабатывает обычное текстовое сообщение
     * 
     * @param message сообщение пользователя
     * @param bot экземпляр бота
     */
    private void handleTextMessage(Message message, TelegramBotService bot) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText();
        
        // Здесь можно добавить логику обработки текстовых сообщений, не являющихся командами
        log.info("Обычное сообщение от пользователя {}: {}", chatId, text);
        bot.sendMessage(chatId, "Я не понимаю ваше сообщение. Используйте /help для просмотра доступных команд.");
    }
    
    /**
     * Обрабатывает привязку Telegram аккаунта
     * 
     * @param message сообщение пользователя
     * @param bot экземпляр бота
     */
    private void handleLinkTelegramState(Message message, TelegramBotService bot) {
        // Логика привязки Telegram аккаунта
        Long chatId = message.getChatId();
        bot.linkTelegram(message);
        userSessionService.clearSession(String.valueOf(chatId));
    }
    
    /**
     * Обрабатывает привязку Discord аккаунта
     * 
     * @param message сообщение пользователя
     * @param bot экземпляр бота
     */
    private void handleLinkDiscordState(Message message, TelegramBotService bot) {
        // Логика привязки Discord аккаунта
        Long chatId = message.getChatId();
        bot.linkDiscord(message);
        userSessionService.clearSession(String.valueOf(chatId));
    }
    
    /**
     * Обрабатывает поиск заказа
     * 
     * @param message сообщение пользователя
     * @param bot экземпляр бота
     */
    private void handleSearchOrderState(Message message, TelegramBotService bot) {
        // Логика поиска заказа
        Long chatId = message.getChatId();
        String searchQuery = message.getText();
        
        // Здесь нужно добавить логику поиска заказа
        bot.sendMessage(String.valueOf(chatId), "Выполняется поиск заказа: " + searchQuery);
        
        userSessionService.clearSession(String.valueOf(chatId));
    }
    
    /**
     * Адаптер для TelegramBotService, который не выполняет никаких действий
     * Используется для обработки сообщений в методе handle
     */
    private static class TelegramBotServiceAdapter extends TelegramBotService {
        public TelegramBotServiceAdapter() {
            super(null, null, null, null, true);
        }
        
        @Override
        public void sendMessage(String chatId, String text) {
            // Ничего не делаем
        }
        
        @Override
        public void linkTelegram(Message message) {
            // Ничего не делаем
        }
        
        @Override
        public void linkDiscord(Message message) {
            // Ничего не делаем
        }
        
        @Override
        public String getBotUsername() {
            return "dummy";
        }
        
        @Override
        public String getBotToken() {
            return "dummy";
        }
    }
} 