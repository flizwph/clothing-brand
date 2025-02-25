package com.brand.backend.TelegramBot;

import com.brand.backend.models.Product;
import com.brand.backend.repositories.ProductRepository;
import com.brand.backend.repositories.UserRepository;
import com.brand.backend.models.User;
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

import java.util.*;

@RequiredArgsConstructor
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

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

        switch (text) {
            case "/start":
                sendMessage(chatId, "👋 Добро пожаловать в наш магазин! Используйте /help для просмотра доступных команд.", getMainMenuButtons());
                break;
            case "/help":
                sendMessage(chatId, "Доступные команды:\n" +
                        "/buy - Купить одежду\n" +
                        "/buyDesktop - Купить desktop-приложение\n" +
                        "/linkTelegram - Привязать Telegram-аккаунт\n" +
                        "/linkDiscord - Привязать Discord-аккаунт (в разработке)");
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
            default:
                sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
        }
    }

    private void linkTelegram(Message message) {
        String chatId = String.valueOf(message.getChatId());
        String text = message.getText();

        Optional<User> userOptional = userRepository.findByVerificationCode(text);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Optional<User> existingUser = userRepository.findByTelegramId(message.getChatId());
            if (existingUser.isPresent() && !existingUser.get().equals(user)) {
                String censoredUsername = TelegramMiscMetods.censorUsername(existingUser.get().getUsername());
                sendMessage(chatId, "Этот Telegram аккаунт уже привязан к пользователю: " + censoredUsername);
                return;
            }

            user.setTelegramId(message.getChatId());
            user.setVerified(true);
            userRepository.save(user);
            sendMessage(chatId, "Аккаунт привязан успешно, теперь делать покупки можно прямо здесь!!!");
        } else {
            sendMessage(chatId, "Код не верен, попробуйте еще раз.");
        }
        userStates.remove(message.getChatId());
    }

    private void handleCallback(String data, Long chatId, Integer messageId) {
        System.out.println("Получен callback: " + data);
        userStates.remove(chatId);

        try {
            if (data.startsWith("page_")) {
                int pageIndex = Integer.parseInt(data.split("_")[1]);
                editProductPage(chatId, messageId, pageIndex);

            } else if (data.startsWith("size_")) {
                String[] parts = data.split("_");
                Long productId = Long.parseLong(parts[1]);
                String size = parts[2];
                sendMessage(String.valueOf(chatId), "Вы выбрали размер: " + size + ". Оплата будет доступна в будущем.");

            } else {
                switch (data) {
                    case "action_buy":
                        showProductPage(String.valueOf(chatId), 0);
                        break;

                    case "action_buyDesktop":
                        sendMessage(String.valueOf(chatId), "💻 Наше desktop-приложение скоро будет доступно! Следите за новостями.");
                        break;

                    case "action_linkTelegram":
                        userStates.put(chatId, "linkTelegram"); // Устанавливаем состояние для привязки Telegram
                        sendMessage(String.valueOf(chatId), "Отправьте код, который вы скопировали на сайте нашего бренда.");
                        break;

                    case "action_linkDiscord":
                        sendMessage(String.valueOf(chatId), "🔗 Привязка Discord-аккаунта скоро будет доступна.");
                        break;

                    default:
                        sendMessage(String.valueOf(chatId), "Неизвестный запрос. Попробуйте снова.");
                        break;
                }
            }
        } catch (NumberFormatException e) {
            sendMessage(String.valueOf(chatId), "Ошибка в обработке данных. Попробуйте снова.");
            e.printStackTrace();
        } catch (Exception e) {
            sendMessage(String.valueOf(chatId), "Произошла ошибка. Попробуйте снова.");
            e.printStackTrace();
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
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createButton("🛒 Купить одежду", "action_buy")));
        rows.add(List.of(createButton("💻 Купить desktop приложение", "action_buyDesktop")));
        rows.add(List.of(createButton("🔗 Привязать Telegram", "action_linkTelegram")));
        rows.add(List.of(createButton("🔗 Привязать Discord", "action_linkDiscord")));

        markup.setKeyboard(rows);
        return markup;
    }

}
