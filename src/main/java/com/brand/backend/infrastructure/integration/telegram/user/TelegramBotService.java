package com.brand.backend.infrastructure.integration.telegram.user;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.brand.backend.application.user.service.VerificationService;

import java.util.*;

@RequiredArgsConstructor
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final VerificationService verificationService;

    @Override
    public String getBotUsername() {
        return "@obl1vium_bot"; // Ваше имя бота
    }

    @Override
    public String getBotToken() {
        return "7966511776:AAH5rqOuMVme5-irMuHKOk2Od88s97oGCJc"; // Токен бота
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery().getData(), update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId());
        }
    }

    private final Map<Long, String> userStates = new HashMap<>();

    private void handleIncomingMessage(Message message) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText();

        if (userStates.containsKey(message.getChatId()) && userStates.get(message.getChatId()).equals("linkTelegram")) {
            linkTelegram(message); // Если пользователь в процессе привязки Telegram
            return;
        }
        
        if (userStates.containsKey(message.getChatId()) && userStates.get(message.getChatId()).equals("linkDiscord")) {
            linkDiscord(message); // Если пользователь в процессе привязки Discord
            return;
        }

        switch (text) {
            case "/start":
                sendMessage(chatId, "👋 Добро пожаловать в наш магазин! Используйте /help для просмотра доступных команд.", getMainMenuButtons());
                break;
            case "/help":
                sendMessage(chatId, "Доступные команды:\n" +
                        "/buy - Купить одежду\n" +
                        "/buyDesktop - Купить desktop-приложение\n" +
                        "/linkTelegram - Привязать Telegram-аккаунт\n" +
                        "/linkDiscord - Привязать Discord-аккаунт");
                break;
            case "/buy":
                showProductPage(chatId, 0);
                break;
            case "/buyDesktop":
                sendMessage(chatId, "💻 Наше desktop-приложение скоро будет доступно! Следите за новостями.");
                break;
            case "/linkTelegram":
                userStates.put(message.getChatId(), "linkTelegram"); // Устанавливаем состояние
                sendMessage(chatId, "Отправьте код, который вы скопировали на сайте нашего бренда.");
                break;
            case "/linkDiscord":
                handleLinkDiscordCommand(message);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
        }
    }

    private void linkTelegram(Message message) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText();

        User user = verificationService.verifyCode(text);
        if (user != null) {
            Optional<User> existingUser = userRepository.findByTelegramId(message.getChatId());
            if (existingUser.isPresent() && !existingUser.get().equals(user)) {
                String censoredUsername = TelegramMiscMetods.censorUsername(existingUser.get().getUsername());
                sendMessage(chatId, "Этот Telegram аккаунт уже привязан к пользователю: " + censoredUsername);
                return;
            }

            user.setTelegramId(message.getChatId());
            // Добавляем парсинг обычного имени пользователя (telegram_username)
            String telegramUsername = message.getFrom().getUserName();
            if (telegramUsername != null && !telegramUsername.isBlank()) {
                user.setTelegramUsername(telegramUsername);
            }
            user.setVerified(true);
            userRepository.save(user);
            sendMessage(chatId, "Аккаунт привязан успешно, теперь делать покупки можно прямо здесь!!!");
        } else {
            sendMessage(chatId, "Код не верен, попробуйте еще раз.");
        }
        userStates.remove(message.getChatId());
    }

    // Обработка команды привязки Discord
    private void handleLinkDiscordCommand(Message message) {
        String chatId = String.valueOf(message.getChatId());
        Long telegramId = message.getChatId();
        
        // Проверяем, привязан ли уже Telegram аккаунт к учетной записи
        Optional<User> userOptional = userRepository.findByTelegramId(telegramId);
        
        if (userOptional.isEmpty()) {
            sendMessage(chatId, "Для привязки Discord сначала привяжите свой Telegram аккаунт к учетной записи на сайте.");
            return;
        }
        
        // Генерируем код верификации для привязки Discord
        String verificationCode = verificationService.generateAndSaveVerificationCodeByTelegramId(telegramId);
        if (verificationCode == null) {
            sendMessage(chatId, "Произошла ошибка при генерации кода. Пожалуйста, попробуйте позже.");
            return;
        }
        
        // Отправляем инструкции и код верификации пользователю
        StringBuilder instructions = new StringBuilder();
        instructions.append("📱 *Как привязать Discord аккаунт:*\n\n");
        instructions.append("1. Добавьте нашего бота в Discord: https://discord.gg/our-bot\n");
        instructions.append("2. Напишите нашему боту команду `!link ").append(verificationCode).append("`\n");
        instructions.append("3. Или отправьте боту команду `!link`, а затем код `").append(verificationCode).append("`\n\n");
        instructions.append("⚠️ Код действителен в течение 10 минут.");
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(instructions.toString());
        sendMessage.enableMarkdown(true);
        
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    // Обработка кода верификации Discord
    private void linkDiscord(Message message) {
        // Мы не реализуем здесь верификацию Discord через Telegram бот,
        // так как для привязки Discord требуется сам Discord-клиент
        // Пользователю будет предоставлен код, который он отправит в Discord боте
        String chatId = String.valueOf(message.getChatId());
        sendMessage(chatId, "Для привязки Discord используйте команду /linkDiscord и следуйте инструкциям.");
        userStates.remove(message.getChatId());
    }

    private void handleCallback(String data, Long chatId, Integer messageId) {
        String stringChatId = chatId.toString();
        
        if (data.equals("shop")) {
            showProductPage(stringChatId, 0);
        } else if (data.equals("help")) {
            sendMessage(stringChatId, "Доступные команды:\n" +
                    "/buy - Купить одежду\n" +
                    "/buyDesktop - Купить desktop-приложение\n" +
                    "/linkTelegram - Привязать Telegram-аккаунт\n" +
                    "/linkDiscord - Привязать Discord-аккаунт");
        } else if (data.equals("startLinkTelegram")) {
            userStates.put(chatId, "linkTelegram");
            sendMessage(stringChatId, "Отправьте код, который вы скопировали на сайте нашего бренда.");
        } else if (data.equals("startLinkDiscord")) {
            handleLinkDiscordCommand(new Message() {
                @Override
                public Long getChatId() {
                    return chatId;
                }
                
                @Override
                public org.telegram.telegrambots.meta.api.objects.User getFrom() {
                    return null; // Не требуется для данного вызова
                }
            });
        } else if (data.startsWith("page_")) {
            int pageIndex = Integer.parseInt(data.substring(5));
            editProductPage(chatId, messageId, pageIndex);
        } else if (data.startsWith("size_")) {
            String[] parts = data.split("_");
            Long productId = Long.parseLong(parts[1]);
            String size = parts[2];
            handleProductSelection(stringChatId, productId, size);
        }
    }

    private void showProductPage(String chatId, int pageIndex) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            sendMessage(chatId, "Извините, товары временно недоступны.");
            return;
        }

        Product product = products.get(pageIndex);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👕 " + product.getName() + "\n💵 Цена: " + product.getPrice() + " RUB");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (pageIndex > 0) {
            rows.add(List.of(createButton("⬅️ Назад", "page_" + (pageIndex - 1))));
        }

        List<InlineKeyboardButton> sizeButtons = new ArrayList<>();
        for (String size : product.getSizes()) {
            sizeButtons.add(createButton(size, "size_" + product.getId() + "_" + size));
        }
        rows.add(sizeButtons);

        if (pageIndex < products.size() - 1) {
            rows.add(List.of(createButton("➡️ Далее", "page_" + (pageIndex + 1))));
        }

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editProductPage(Long chatId, Integer messageId, int pageIndex) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }

        Product product = products.get(pageIndex);
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText("👕 " + product.getName() + "\n💵 Цена: " + product.getPrice() + " USD");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (pageIndex > 0) {
            rows.add(List.of(createButton("⬅️ Назад", "page_" + (pageIndex - 1))));
        }

        List<InlineKeyboardButton> sizeButtons = new ArrayList<>();
        for (String size : product.getSizes()) {
            sizeButtons.add(createButton(size, "size_" + product.getId() + "_" + size));
        }
        rows.add(sizeButtons);

        if (pageIndex < products.size() - 1) {
            rows.add(List.of(createButton("➡️ Далее", "page_" + (pageIndex + 1))));
        }

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup getMainMenuButtons() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Первый ряд кнопок
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("👕 Магазин", "shop"));
        row1.add(createButton("💬 Помощь", "help"));
        rows.add(row1);
        
        // Второй ряд кнопок - кнопки привязки аккаунтов
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔗 Привязать Telegram", "startLinkTelegram"));
        row2.add(createButton("🔗 Привязать Discord", "startLinkDiscord"));
        rows.add(row2);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private void handleProductSelection(String chatId, Long productId, String size) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            String message = "Вы выбрали: " + product.getName() + "\n" +
                    "Размер: " + size + "\n" +
                    "Цена: " + product.getPrice() + " USD\n\n" +
                    "Оплата будет доступна в будущем.";
            
            sendMessage(chatId, message);
        } else {
            sendMessage(chatId, "Товар не найден. Попробуйте выбрать другой товар.");
        }
    }

}

