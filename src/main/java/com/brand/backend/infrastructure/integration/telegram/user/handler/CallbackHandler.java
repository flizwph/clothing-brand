package com.brand.backend.infrastructure.integration.telegram.user.handler;

import com.brand.backend.infrastructure.integration.telegram.user.command.CartCommand;
import com.brand.backend.infrastructure.integration.telegram.user.service.CartService;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramProductService;
import com.brand.backend.infrastructure.integration.telegram.user.service.TelegramBotService;
import com.brand.backend.infrastructure.integration.telegram.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик callback-запросов
 */
@Component
@RequiredArgsConstructor
public class CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CallbackHandler.class);
    
    private final TelegramProductService productService;
    private final CartService cartService;
    private final UserSessionService userSessionService;
    private final CartCommand cartCommand;

    /**
     * Обрабатывает callback-запрос и возвращает ответ для выполнения
     * 
     * @param callbackQuery callback-запрос
     * @return ответное сообщение или null
     */
    public BotApiMethod<?> handle(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        
        log.info("Получен callback от чата {}: {}", chatId, callbackData);
        
        // Используем адаптер TelegramBotService для обработки callback
        TelegramBotService dummyBot = new TelegramBotServiceAdapter();
        handleCallback(callbackData, chatId, messageId, dummyBot);
        
        // В большинстве случаев здесь возвращаем null, так как callback-обработчики 
        // формируют и отправляют ответы самостоятельно
        return null;
    }

    /**
     * Обрабатывает callback-запрос
     * 
     * @param callbackData данные callback
     * @param chatId ID чата
     * @param messageId ID сообщения
     * @param bot экземпляр бота
     */
    public void handleCallback(String callbackData, Long chatId, Integer messageId, TelegramBotService bot) {
        log.info("Handling callback from chat {}: {}", chatId, callbackData);
        
        String stringChatId = chatId.toString();
        
        if (callbackData.equals("shop_categories")) {
            handleShopCategoriesCallback(stringChatId, bot);
        } else if (callbackData.equals("shop_category_clothes")) {
            handleClothesCategoryCallback(stringChatId, bot);
        } else if (callbackData.equals("shop_category_accessories")) {
            handleAccessoriesCategoryCallback(stringChatId, bot);
        } else if (callbackData.equals("shop_category_desktop")) {
            handleDesktopCategoryCallback(stringChatId, bot);
        } else if (callbackData.equals("coming_soon")) {
            handleComingSoonCallback(stringChatId, bot);
        } else if (callbackData.startsWith("desktop_")) {
            handleDesktopPlanCallback(callbackData, stringChatId, bot);
        } else if (callbackData.equals("shop")) {
            handleShopCallback(stringChatId, bot);
        } else if (callbackData.equals("help")) {
            handleHelpCallback(stringChatId, bot);
        } else if (callbackData.equals("main_menu")) {
            handleMainMenuCallback(stringChatId, bot);
        } else if (callbackData.equals("startLinkTelegram")) {
            handleLinkTelegramCallback(chatId, stringChatId, bot);
        } else if (callbackData.equals("startLinkDiscord")) {
            handleLinkDiscordCallback(stringChatId, bot);
        } else if (callbackData.startsWith("page_")) {
            handlePageCallback(callbackData, chatId, messageId, bot);
        } else if (callbackData.startsWith("size_")) {
            handleSizeCallback(callbackData, stringChatId, bot);
        } else if (callbackData.startsWith("add_to_cart_")) {
            handleAddToCartCallback(callbackData, stringChatId, bot);
        } else if (callbackData.equals("view_cart")) {
            handleViewCartCallback(stringChatId, bot);
        } else if (callbackData.startsWith("remove_from_cart_")) {
            handleRemoveFromCartCallback(callbackData, stringChatId, bot);
        } else if (callbackData.equals("checkout")) {
            handleCheckoutCallback(stringChatId, bot);
        } else if (callbackData.equals("clear_cart")) {
            handleClearCartCallback(chatId, bot);
        } else if (callbackData.startsWith("cart_increase_")) {
            handleCartIncreaseCallback(callbackData, chatId, bot);
        } else if (callbackData.startsWith("cart_decrease_")) {
            handleCartDecreaseCallback(callbackData, chatId, bot);
        } else if (callbackData.startsWith("cart_remove_")) {
            handleCartRemoveCallback(callbackData, chatId, bot);
        } else {
            log.warn("Unknown callback data: {}", callbackData);
            bot.sendMessage(stringChatId, "Неизвестная команда. Используйте /help для просмотра доступных команд.");
        }
    }
    
    private void handleShopCategoriesCallback(String chatId, TelegramBotService bot) {
        bot.showShopCategories(chatId);
    }
    
    private void handleClothesCategoryCallback(String chatId, TelegramBotService bot) {
        bot.showClothesCategory(chatId);
    }
    
    private void handleAccessoriesCategoryCallback(String chatId, TelegramBotService bot) {
        bot.showAccessoriesCategory(chatId);
    }
    
    private void handleDesktopCategoryCallback(String chatId, TelegramBotService bot) {
        bot.showDesktopAppCategory(chatId);
    }
    
    private void handleComingSoonCallback(String chatId, TelegramBotService bot) {
        bot.showComingSoon(chatId);
    }
    
    private void handleDesktopPlanCallback(String callbackData, String chatId, TelegramBotService bot) {
        // Обработка callback'ов для desktop приложения
        if (callbackData.equals("desktop_basic")) {
            bot.showDesktopPlan(chatId, "basic");
        } else if (callbackData.equals("desktop_standard")) {
            bot.showDesktopPlan(chatId, "standard");
        } else if (callbackData.equals("desktop_premium")) {
            bot.showDesktopPlan(chatId, "premium");
        } else if (callbackData.startsWith("desktop_buy_")) {
            // Обработка покупки подписки: desktop_buy_plan_duration
            String[] parts = callbackData.split("_");
            if (parts.length >= 4) {
                String plan = parts[2];
                int duration = Integer.parseInt(parts[3]);
                handleDesktopSubscriptionPurchase(chatId, plan, duration, bot);
            }
        }
    }
    
    private void handleDesktopSubscriptionPurchase(String chatId, String plan, int duration, TelegramBotService bot) {
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
        
        bot.sendMessage(chatId, message, markup);
    }
    
    private void handleShopCallback(String chatId, TelegramBotService bot) {
        bot.showProductPage(chatId, 0);
    }
    
    private void handleHelpCallback(String chatId, TelegramBotService bot) {
        String helpMessage = """
                Доступные команды:
                
                /buy - Купить одежду
                /cart - Корзина покупок
                /linkTelegram - Привязать Telegram аккаунт
                /linkDiscord - Привязать Discord аккаунт
                /help - Помощь
                
                Также вы можете использовать кнопки меню для навигации.
                """;
        bot.sendMessage(chatId, helpMessage);
    }
    
    private void handleMainMenuCallback(String chatId, TelegramBotService bot) {
        String welcomeMessage = """
                👋 Добро пожаловать в наш магазин!
                
                Используйте команды ниже для навигации:
                """;
        bot.sendMessage(chatId, welcomeMessage, bot.getMainMenuButtons());
    }
    
    private void handleLinkTelegramCallback(Long chatId, String stringChatId, TelegramBotService bot) {
        userSessionService.setUserState(chatId, "linkTelegram");
        String message = """
                🔗 *Привязка Telegram аккаунта*
                
                Для привязки аккаунта вам необходимо:
                1. Войти на сайт нашего бренда
                2. Перейти в раздел "Настройки профиля"
                3. Нажать кнопку "Привязать Telegram"
                4. Скопировать показанный код
                5. Отправить этот код сюда
                
                ⚠️ Код действителен в течение 10 минут
                """;
        bot.sendMessage(stringChatId, message);
    }
    
    private void handleLinkDiscordCallback(String chatId, TelegramBotService bot) {
        bot.handleLinkDiscordCommand(chatId);
    }
    
    private void handlePageCallback(String callbackData, Long chatId, Integer messageId, TelegramBotService bot) {
        int pageIndex = Integer.parseInt(callbackData.substring(5));
        bot.editProductPage(chatId, messageId, pageIndex);
    }
    
    private void handleSizeCallback(String callbackData, String chatId, TelegramBotService bot) {
        String[] parts = callbackData.split("_");
        Long productId = Long.parseLong(parts[1]);
        String size = parts[2];
        productService.handleProductSelection(chatId, productId, size, bot);
    }
    
    private void handleAddToCartCallback(String callbackData, String chatId, TelegramBotService bot) {
        String[] parts = callbackData.split("_");
        Long productId = Long.parseLong(parts[1]);
        String size = parts.length > 2 ? parts[2] : "M"; // Если размер не указан, используем размер по умолчанию
        cartService.addProductToCart(Long.parseLong(chatId), productId, size, 1);
        bot.sendMessage(chatId, "✅ Товар добавлен в корзину! Используйте /cart для просмотра корзины.");
    }
    
    private void handleViewCartCallback(String chatId, TelegramBotService bot) {
        cartCommand.showCart(Long.parseLong(chatId));
    }
    
    private void handleRemoveFromCartCallback(String callbackData, String chatId, TelegramBotService bot) {
        int itemIndex = Integer.parseInt(callbackData.substring(17));
        cartCommand.handleRemoveItem(Long.parseLong(chatId), itemIndex);
    }
    
    private void handleCheckoutCallback(String chatId, TelegramBotService bot) {
        bot.sendMessage(chatId, "🛍️ Оформление заказа будет доступно в ближайшее время!");
    }
    
    private void handleClearCartCallback(Long chatId, TelegramBotService bot) {
        cartCommand.handleClearCart(chatId);
    }
    
    private void handleCartIncreaseCallback(String callbackData, Long chatId, TelegramBotService bot) {
        int itemIndex = Integer.parseInt(callbackData.substring(14));
        cartCommand.handleIncreaseQuantity(chatId, itemIndex);
    }
    
    private void handleCartDecreaseCallback(String callbackData, Long chatId, TelegramBotService bot) {
        int itemIndex = Integer.parseInt(callbackData.substring(14));
        cartCommand.handleDecreaseQuantity(chatId, itemIndex);
    }
    
    private void handleCartRemoveCallback(String callbackData, Long chatId, TelegramBotService bot) {
        int itemIndex = Integer.parseInt(callbackData.substring(12));
        cartCommand.handleRemoveItem(chatId, itemIndex);
    }
    
    /**
     * Адаптер для TelegramBotService, который не выполняет никаких действий
     * Используется для обработки callback в методе handle
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
        public void sendMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup) {
            // Ничего не делаем
        }
        
        @Override
        public void editProductPage(Long chatId, Integer messageId, int pageIndex) {
            // Ничего не делаем
        }
        
        @Override
        public void showProductPage(String chatId, int pageIndex) {
            // Ничего не делаем
        }
        
        @Override
        public void handleLinkDiscordCommand(String chatId) {
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
    
    /**
     * Создает кнопку с заданным текстом и callback-данными
     * 
     * @param text текст кнопки
     * @param callbackData данные callback
     * @return созданная кнопка
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
} 