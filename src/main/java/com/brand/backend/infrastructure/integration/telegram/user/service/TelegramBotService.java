package com.brand.backend.infrastructure.integration.telegram.user.service;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.domain.user.repository.UserRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.infrastructure.integration.telegram.user.util.TelegramMiscMetods;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final VerificationService verificationService;
    private final TelegramProductService telegramProductService;
    
    @org.springframework.beans.factory.annotation.Autowired
    public TelegramBotService(UserRepository userRepository, 
                              ProductRepository productRepository, 
                              VerificationService verificationService,
                              TelegramProductService telegramProductService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.verificationService = verificationService;
        this.telegramProductService = telegramProductService;
    }
    
    protected TelegramBotService(UserRepository userRepository, 
                               ProductRepository productRepository,
                               VerificationService verificationService, 
                               TelegramProductService telegramProductService, 
                               boolean dummy) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.verificationService = verificationService;
        this.telegramProductService = telegramProductService;
    }

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
                String helpMessage = """
                        Доступные команды:
                        
                        /buy - Купить одежду
                        /cart - Корзина покупок
                        /linkTelegram - Привязать Telegram аккаунт
                        /linkDiscord - Привязать Discord аккаунт
                        /help - Помощь
                        
                        Также вы можете использовать кнопки меню для навигации.
                        """;
                sendMessage(chatId, helpMessage);
                break;
            case "/buy":
                showShopCategories(chatId);
                break;
            case "/cart":
                // Выполнить команду просмотра корзины
                // (должна быть реализована в CartCommand)
                sendMessage(chatId, "🛒 Переход к корзине...");
                break;
            case "/buyDesktop":
                showDesktopAppCategory(chatId);
                break;
            case "/linkTelegram":
                userStates.put(message.getChatId(), "linkTelegram"); // Устанавливаем состояние
                String message1 = """
                        🔗 *Привязка Telegram аккаунта*
                        
                        Для привязки аккаунта вам необходимо:
                        1. Войти на сайт нашего бренда
                        2. Перейти в раздел "Настройки профиля"
                        3. Нажать кнопку "Привязать Telegram"
                        4. Скопировать показанный код
                        5. Отправить этот код сюда
                        
                        ⚠️ Код действителен в течение 10 минут
                        """;
                sendMessage(chatId, message1);
                break;
            case "/linkDiscord":
                handleLinkDiscordCommand(chatId);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
        }
    }

    private void handleCallback(String data, Long chatId, Integer messageId) {
        String stringChatId = chatId.toString();
        
        if (data.equals("shop_categories")) {
            showShopCategories(stringChatId);
        } else if (data.equals("shop_category_clothes")) {
            showClothesCategory(stringChatId);
        } else if (data.equals("shop_category_accessories")) {
            showAccessoriesCategory(stringChatId);
        } else if (data.equals("shop_category_desktop")) {
            showDesktopAppCategory(stringChatId);
        } else if (data.equals("coming_soon")) {
            showComingSoon(stringChatId);
        } else if (data.startsWith("desktop_")) {
            handleDesktopCallback(data, stringChatId);
        } else if (data.equals("shop")) {
            showProductPage(stringChatId, 0);
        } else if (data.equals("help")) {
            sendMessage(stringChatId, "Доступные команды:\n" +
                    "/buy - Купить одежду\n" +
                    "/buyDesktop - Купить desktop-приложение\n" +
                    "/linkTelegram - Привязать Telegram-аккаунт\n" +
                    "/linkDiscord - Привязать Discord-аккаунт");
        } else if (data.equals("main_menu")) {
            sendMessage(stringChatId, "👋 Добро пожаловать в наш магазин! Используйте /help для просмотра доступных команд.", getMainMenuButtons());
        } else if (data.equals("startLinkTelegram")) {
            userStates.put(chatId, "linkTelegram");
            sendMessage(stringChatId, "Отправьте код, который вы скопировали на сайте нашего бренда.");
        } else if (data.equals("startLinkDiscord")) {
            handleLinkDiscordCommand(stringChatId);
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

    /**
     * Обрабатывает callback-запросы, связанные с desktop-приложением
     * 
     * @param data данные callback
     * @param chatId ID чата
     */
    private void handleDesktopCallback(String data, String chatId) {
        if (data.equals("desktop_basic")) {
            showDesktopPlan(chatId, "basic");
        } else if (data.equals("desktop_standard")) {
            showDesktopPlan(chatId, "standard");
        } else if (data.equals("desktop_premium")) {
            showDesktopPlan(chatId, "premium");
        } else if (data.startsWith("desktop_buy_")) {
            // Пример: desktop_buy_basic_1
            String[] parts = data.split("_");
            if (parts.length >= 4) {
                String plan = parts[2];
                int duration = Integer.parseInt(parts[3]);
                handleDesktopSubscriptionPurchase(chatId, plan, duration);
            }
        }
    }
    
    /**
     * Обрабатывает покупку подписки на desktop-приложение
     * 
     * @param chatId ID чата
     * @param plan тип плана
     * @param duration длительность подписки в месяцах
     */
    private void handleDesktopSubscriptionPurchase(String chatId, String plan, int duration) {
        String planName;
        int pricePerMonth;
        
        switch (plan) {
            case "basic":
                planName = "Базовый";
                pricePerMonth = 99;
                break;
            case "standard":
                planName = "Стандарт";
                pricePerMonth = 199;
                break;
            case "premium":
                planName = "Премиум";
                pricePerMonth = 299;
                break;
            default:
                planName = "Неизвестный";
                pricePerMonth = 0;
                break;
        }
        
        double discount = 0;
        if (duration == 3) {
            discount = 0.1; // 10% скидка
        } else if (duration == 12) {
            discount = 0.2; // 20% скидка
        }
        
        double totalPrice = pricePerMonth * duration * (1 - discount);
        
        String message = String.format("""
                🛒 *Оформление подписки*
                
                План: %s
                Срок: %d месяц%s
                Цена за месяц: %d₽
                Скидка: %.0f%%
                
                *Итого к оплате: %.0f₽*
                
                Оплата будет доступна в ближайшее время!
                """, 
                planName, 
                duration, 
                duration == 1 ? "" : (duration < 5 ? "а" : "ев"), 
                pricePerMonth,
                discount * 100,
                totalPrice);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопки навигации
        rows.add(List.of(createButton("🔙 Назад к планам", "shop_category_desktop")));
        rows.add(List.of(createButton("🏠 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }

    public void handleLinkDiscordCommand(String chatId) {
        Long telegramId = Long.parseLong(chatId);
        
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
    
    public void showProductPage(String chatId, int pageIndex) {
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

    public void editProductPage(Long chatId, Integer messageId, int pageIndex) {
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

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String chatId, String text, InlineKeyboardMarkup markup) {
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

    /**
     * Возвращает клавиатуру с кнопками главного меню
     * 
     * @return клавиатура с кнопками
     */
    public InlineKeyboardMarkup getMainMenuButtons() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Первый ряд кнопок: магазин и корзина
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🛍️ Магазин", "shop_categories"));
        row1.add(createButton("🛒 Корзина", "view_cart"));
        rows.add(row1);
        
        // Второй ряд кнопок: привязка аккаунтов
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔗 Привязать Telegram", "startLinkTelegram"));
        row2.add(createButton("🔗 Привязать Discord", "startLinkDiscord"));
        rows.add(row2);
        
        // Третий ряд кнопок: помощь
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("❓ Помощь", "help"));
        rows.add(row3);
        
        markup.setKeyboard(rows);
        return markup;
    }

    private void handleProductSelection(String chatId, Long productId, String size) {
        Product product = telegramProductService.getProductById(productId);
        if (product != null) {
            String message = "Вы выбрали: " + product.getName() + "\n" +
                    "Размер: " + size + "\n" +
                    "Цена: " + product.getPrice() + " USD\n\n" +
                    "Оплата будет доступна в будущем.";
            
            sendMessage(chatId, message);
        } else {
            sendMessage(chatId, "Товар не найден. Попробуйте выбрать другой товар.");
        }
    }

    public void linkTelegram(Message message) {
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

    public void linkDiscord(Message message) {
        // Мы не реализуем здесь верификацию Discord через Telegram бот,
        // так как для привязки Discord требуется сам Discord-клиент
        // Пользователю будет предоставлен код, который он отправит в Discord боте
        String chatId = String.valueOf(message.getChatId());
        sendMessage(chatId, "Для привязки Discord используйте команду /linkDiscord и следуйте инструкциям.");
        userStates.remove(message.getChatId());
    }

    public void searchUser(String chatId, String username) {
        // Изменяем логику поиска на использование имеющихся методов репозитория
        List<User> users = userRepository.findAll().stream()
                .filter(user -> user.getUsername() != null && 
                        user.getUsername().toLowerCase().contains(username.toLowerCase()))
                .collect(Collectors.toList());
        
        if (users.isEmpty()) {
            sendMessage(chatId, "🔍 Пользователи не найдены");
            return;
        }
        
        StringBuilder messageText = new StringBuilder("🔍 Результаты поиска пользователей:\n\n");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        for (User user : users) {
            String userInfo = user.getUsername() + (user.isVerified() ? " ✅" : " ❌");
            messageText.append("👤 ").append(userInfo).append("\n");
            
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(createButton("👤 " + user.getUsername(), "user_details_" + user.getId()));
            rows.add(row);
        }
        
        // Добавляем кнопку возврата в главное меню
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createButton("🔙 Назад", "main_menu"));
        rows.add(backRow);
        
        markup.setKeyboard(rows);
        sendMessage(chatId, messageText.toString(), markup);
    }

    public void showUserDetails(String chatId, Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "⚠️ Пользователь не найден");
            return;
        }
        
        User user = userOpt.get();
        StringBuilder messageText = new StringBuilder("👤 Информация о пользователе:\n\n");
        messageText.append("Имя пользователя: ").append(user.getUsername()).append("\n");
        messageText.append("Email: ").append(user.getEmail()).append("\n");
        messageText.append("Статус: ").append(user.isVerified() ? "Подтвержден ✅" : "Не подтвержден ❌").append("\n");
        
        if (user.getTelegramId() != null) {
            messageText.append("Telegram: Привязан ✅\n");
        } else {
            messageText.append("Telegram: Не привязан ❌\n");
        }
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопка возврата к поиску
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createButton("🔙 Назад", "search_users"));
        rows.add(backRow);
        
        markup.setKeyboard(rows);
        sendMessage(chatId, messageText.toString(), markup);
    }

    /**
     * Показывает категории товаров
     * 
     * @param chatId ID чата
     */
    public void showShopCategories(String chatId) {
        String message = """
                🛍️ *Категории товаров*
                
                Выберите категорию товаров из списка ниже:
                """;
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопки категорий
        rows.add(List.of(createButton("👕 Одежда", "shop_category_clothes")));
        rows.add(List.of(createButton("🧢 Аксессуары", "shop_category_accessories")));
        rows.add(List.of(createButton("💻 Desktop приложение", "shop_category_desktop")));
        
        // Кнопка возврата в главное меню
        rows.add(List.of(createButton("🔙 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }
    
    /**
     * Показывает категорию одежды
     * 
     * @param chatId ID чата
     */
    public void showClothesCategory(String chatId) {
        String message = """
                👕 *Одежда*
                
                Выберите тип одежды:
                """;
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Подкатегории одежды
        rows.add(List.of(createButton("👕 Футболки", "shop")));
        rows.add(List.of(createButton("👖 Штаны (Скоро)", "coming_soon")));
        
        // Кнопки навигации
        rows.add(List.of(createButton("🔙 Назад к категориям", "shop_categories")));
        rows.add(List.of(createButton("🏠 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }
    
    /**
     * Показывает категорию аксессуаров
     * 
     * @param chatId ID чата
     */
    public void showAccessoriesCategory(String chatId) {
        String message = """
                🧢 *Аксессуары*
                
                В данный момент аксессуары находятся в разработке и будут доступны в ближайшее время.
                
                Следите за обновлениями!
                """;
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопки навигации
        rows.add(List.of(createButton("🔙 Назад к категориям", "shop_categories")));
        rows.add(List.of(createButton("🏠 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }
    
    /**
     * Показывает категорию desktop приложения с планами подписки
     * 
     * @param chatId ID чата
     */
    public void showDesktopAppCategory(String chatId) {
        String message = """
                💻 *Desktop приложение*
                
                Наше приложение доступно по подписке.
                Выберите подходящий вам тарифный план:
                """;
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Тарифные планы
        rows.add(List.of(createButton("🥉 Базовый (99₽/мес)", "desktop_basic")));
        rows.add(List.of(createButton("🥈 Стандарт (199₽/мес)", "desktop_standard")));
        rows.add(List.of(createButton("🥇 Премиум (299₽/мес)", "desktop_premium")));
        
        // Длительность подписки
        List<InlineKeyboardButton> durationRow = new ArrayList<>();
        durationRow.add(createButton("1 месяц", "desktop_duration_1"));
        durationRow.add(createButton("3 месяца", "desktop_duration_3"));
        durationRow.add(createButton("12 месяцев", "desktop_duration_12"));
        rows.add(durationRow);
        
        // Кнопки навигации
        rows.add(List.of(createButton("🔙 Назад к категориям", "shop_categories")));
        rows.add(List.of(createButton("🏠 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }
    
    /**
     * Отображает сообщение "Скоро появится"
     * 
     * @param chatId ID чата
     */
    public void showComingSoon(String chatId) {
        String message = """
                🔜 *Скоро появится!*
                
                Данная функция или товар находится в разработке и будет доступен в ближайшее время.
                
                Следите за обновлениями!
                """;
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Кнопки навигации
        rows.add(List.of(createButton("🔙 Назад к категориям", "shop_categories")));
        rows.add(List.of(createButton("🏠 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }
    
    /**
     * Показывает информацию о плане подписки desktop приложения
     * 
     * @param chatId ID чата
     * @param plan тип плана
     */
    public void showDesktopPlan(String chatId, String plan) {
        String planName;
        int pricePerMonth;
        String features;
        
        switch (plan) {
            case "basic":
                planName = "Базовый";
                pricePerMonth = 99;
                features = """
                        • Базовый функционал
                        • Доступ к сообществу
                        • 1 проект
                        """;
                break;
            case "standard":
                planName = "Стандарт";
                pricePerMonth = 199;
                features = """
                        • Расширенный функционал
                        • Доступ к сообществу
                        • 5 проектов
                        • Приоритетная поддержка
                        """;
                break;
            case "premium":
                planName = "Премиум";
                pricePerMonth = 299;
                features = """
                        • Полный функционал
                        • Доступ к сообществу
                        • Неограниченное количество проектов
                        • Приоритетная поддержка 24/7
                        • Эксклюзивные обновления
                        """;
                break;
            default:
                planName = "Неизвестный";
                pricePerMonth = 0;
                features = "Информация недоступна";
        }
        
        String message = String.format("""
                💻 *План подписки: %s*
                
                Стоимость: %d₽ в месяц
                
                Особенности:
                %s
                
                Выберите длительность подписки для продолжения:
                """, planName, pricePerMonth, features);
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        // Длительность подписки
        List<InlineKeyboardButton> durationRow = new ArrayList<>();
        durationRow.add(createButton("1 месяц", "desktop_buy_" + plan + "_1"));
        durationRow.add(createButton("3 месяца (-10%)", "desktop_buy_" + plan + "_3"));
        durationRow.add(createButton("12 месяцев (-20%)", "desktop_buy_" + plan + "_12"));
        rows.add(durationRow);
        
        // Кнопки навигации
        rows.add(List.of(createButton("🔙 Назад к планам", "shop_category_desktop")));
        rows.add(List.of(createButton("🏠 Главное меню", "main_menu")));
        
        markup.setKeyboard(rows);
        
        sendMessage(chatId, message, markup);
    }

}

