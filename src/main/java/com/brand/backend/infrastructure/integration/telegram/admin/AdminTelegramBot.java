package com.brand.backend.infrastructure.integration.telegram.admin;

import com.brand.backend.infrastructure.integration.telegram.admin.handlers.OrderHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.UserHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.PromoCodeHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.ProductHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.keyboards.AdminKeyboards;
import com.brand.backend.infrastructure.integration.telegram.admin.service.AdminBotService;
import com.brand.backend.domain.order.model.OrderStatus;
import com.brand.backend.domain.order.repository.OrderRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.nft.model.NFT;
import com.brand.backend.domain.payment.model.Transaction;
import com.brand.backend.domain.payment.model.TransactionStatus;
import com.brand.backend.domain.payment.repository.TransactionRepository;
import com.brand.backend.application.payment.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
public class AdminTelegramBot extends TelegramLongPollingBot {

    private final OrderRepository orderRepository;
    private final OrderHandler orderHandler;
    private final UserHandler userHandler;
    private final AdminBotService adminBotService;
    private final PromoCodeHandler promoCodeHandler;
    private final ProductHandler productHandler;
    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdminTelegramBot.class);

    // Состояния пользователей (chatId -> UserState)
    private final Map<String, UserState> userStates = new ConcurrentHashMap<>();
    
    // Возможные состояния пользователя
    private enum UserState {
        NONE,               // Обычный режим
        WAITING_ORDER_SEARCH // Ожидание ввода запроса для поиска заказа
    }

    @Value("${admin.bot.username}")
    private String botUsername;

    // Список разрешенных Telegram ID, заданный в конфигурационном файле (через запятую)
    @Value("${admin.bot.adminIds}")
    private String adminIds;

    private Set<String> allowedAdminIds;
    
    // Регулярное выражение для обработки команд вида /order_123
    private static final Pattern ORDER_COMMAND_PATTERN = Pattern.compile("/order(?:_|\\s+)(\\d+)");
    // Регулярное выражение для обработки команды /orders
    private static final Pattern ORDERS_LIST_PATTERN = Pattern.compile("/orders");
    // Регулярное выражение для обработки команд вида /promo_123
    private static final Pattern PROMO_COMMAND_PATTERN = Pattern.compile("/promo(?:s)?(?:_(\\d+))?");
    // Регулярное выражение для обработки команд вида /product_123
    private static final Pattern PRODUCT_COMMAND_PATTERN = Pattern.compile("/product_(\\d+)");
    // Регулярное выражение для обработки команд вида /product_search query
    private static final Pattern PRODUCT_SEARCH_PATTERN = Pattern.compile("/product_search\\s+(.+)");
    // Регулярное выражение для обработки команд вида /product_price_123 1000.50
    private static final Pattern PRODUCT_PRICE_PATTERN = Pattern.compile("/product_price_(\\d+)\\s+(.+)");
    // Регулярное выражение для обработки команд вида /product_stock_123 10 20 30
    private static final Pattern PRODUCT_STOCK_PATTERN = Pattern.compile("/product_stock_(\\d+)\\s+(.+)");
    // Регулярное выражение для обработки команд вида /product_create ProductName 1000.50 10 20 30
    private static final Pattern PRODUCT_CREATE_PATTERN = Pattern.compile("/product_create\\s+(.+)");
    // Регулярное выражение для обработки команд вида /order_search query
    private static final Pattern ORDER_SEARCH_PATTERN = Pattern.compile("/order_search\\s+(.+)");
    // Регулярное выражение для обработки команд вида /promo_create CODE 15% 100 Description
    private static final Pattern PROMO_CREATE_PATTERN = Pattern.compile("^/promo_create\\s+(\\S+)\\s+(\\d+)%\\s+(\\d+)(?:\\s+(.+))?$");
    // Регулярное выражение для обработки команд вида /promo_edit_123 CODE 15% 100 Description
    private static final Pattern PROMO_EDIT_PATTERN = Pattern.compile("^/promo_edit_(\\d+)\\s+([A-Z0-9]+)\\s+(\\d+)%\\s+(\\d+)\\s+(.+)$");
    // Регулярное выражение для обработки команд вида /user_search query
    private static final Pattern USER_SEARCH_PATTERN = Pattern.compile("^/user_search (.+)$");
    // Регулярное выражение для обработки команд вида /usersearch query (альтернативная форма)
    private static final Pattern USER_SEARCH_ALT_PATTERN = Pattern.compile("^/usersearch (.+)$");
    // Регулярное выражение для обработки команд вида /user_123
    private static final Pattern USER_COMMAND_PATTERN = Pattern.compile("/user(?:s)?\\s*(\\d*)");
    // Регулярное выражение для обработки команд вида /name Имя пользователя
    private static final Pattern USER_NAME_SEARCH_PATTERN = Pattern.compile("^/name (.+)$");
    // Регулярное выражение для обработки команд вида /search_user Имя пользователя
    private static final Pattern USER_SEARCH_NAME_PATTERN = Pattern.compile("^/search_user (.+)$");
    // Регулярное выражение для обработки команд вида /email адрес@почты.com
    private static final Pattern USER_EMAIL_SEARCH_PATTERN = Pattern.compile("^/email (.+)$");
    // Регулярное выражение для обработки команд вида /phone +79991234567
    private static final Pattern USER_PHONE_SEARCH_PATTERN = Pattern.compile("^/phone (.+)$");

    // Конструктор с инициализацией всех необходимых полей
    public AdminTelegramBot(
            OrderRepository orderRepository,
            OrderHandler orderHandler,
            UserHandler userHandler,
            AdminBotService adminBotService,
            PromoCodeHandler promoCodeHandler,
            ProductHandler productHandler,
            TransactionRepository transactionRepository,
            BalanceService balanceService,
            @Value("${admin.bot.token}") String botToken) {
        super(botToken); // Передаем токен в конструктор суперкласса
        this.orderRepository = orderRepository;
        this.orderHandler = orderHandler;
        this.userHandler = userHandler;
        this.adminBotService = adminBotService;
        this.promoCodeHandler = promoCodeHandler;
        this.productHandler = productHandler;
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
    }

    @PostConstruct
    public void init() {
        // Инициализация списка администраторов
        String[] adminIdsArray = adminIds.split(",");
        Set<String> adminIdSet = new HashSet<>();
        
        for (String id : adminIdsArray) {
            String trimmedId = id.trim();
            if (!trimmedId.isEmpty()) {
                adminIdSet.add(trimmedId);
                log.info("Добавлен администратор с ID: {}", trimmedId);
            }
        }
        
        allowedAdminIds = adminIdSet;
        log.info("Всего администраторов: {}", allowedAdminIds.size());
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            // Если получено сообщение с текстом
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
            // Если пришел callback от нажатой inline-кнопки
            else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка текстовых сообщений
     */
    private void handleMessage(Message message) {
        String chatId = message.getChatId().toString();
        
        // Проверяем, что отправитель - администратор
        if (!isAdmin(chatId)) {
            sendMessage(chatId, "Доступ запрещен.");
            return;
        }
        
        String text = message.getText();
        
        // Проверяем, находится ли пользователь в режиме поиска заказа
        if (getUserState(chatId) == UserState.WAITING_ORDER_SEARCH) {
            // Обрабатываем поисковый запрос
            log.info("Обработка поискового запроса заказа от админа {}: {}", chatId, text);
            BotApiMethod<?> response = orderHandler.handleOrderSearch(chatId, text);
            executeMethod(response);
            // Сбрасываем состояние после выполнения поиска
            setUserState(chatId, UserState.NONE);
            return;
        }
        
        BotApiMethod<?> response = null;
        
        // Проверяем, соответствует ли текст команде для просмотра заказа
        Matcher orderMatcher = ORDER_COMMAND_PATTERN.matcher(text);
        Matcher ordersListMatcher = ORDERS_LIST_PATTERN.matcher(text);
        Matcher promoMatcher = PROMO_COMMAND_PATTERN.matcher(text);
        Matcher productMatcher = PRODUCT_COMMAND_PATTERN.matcher(text);
        Matcher productSearchMatcher = PRODUCT_SEARCH_PATTERN.matcher(text);
        Matcher productPriceMatcher = PRODUCT_PRICE_PATTERN.matcher(text);
        Matcher productStockMatcher = PRODUCT_STOCK_PATTERN.matcher(text);
        Matcher productCreateMatcher = PRODUCT_CREATE_PATTERN.matcher(text);
        Matcher orderSearchMatcher = ORDER_SEARCH_PATTERN.matcher(text);
        Matcher promoCreateMatcher = PROMO_CREATE_PATTERN.matcher(text);
        Matcher promoEditMatcher = PROMO_EDIT_PATTERN.matcher(text);
        Matcher userSearchMatcher = USER_SEARCH_PATTERN.matcher(text);
        Matcher userSearchAltMatcher = USER_SEARCH_ALT_PATTERN.matcher(text);
        Matcher userMatcher = USER_COMMAND_PATTERN.matcher(text);
        Matcher userNameSearchMatcher = USER_NAME_SEARCH_PATTERN.matcher(text);
        Matcher userSearchNameMatcher = USER_SEARCH_NAME_PATTERN.matcher(text);
        Matcher userEmailSearchMatcher = USER_EMAIL_SEARCH_PATTERN.matcher(text);
        Matcher userPhoneSearchMatcher = USER_PHONE_SEARCH_PATTERN.matcher(text);
        
        if (orderMatcher.matches()) {
            response = handleOrderCommand(chatId, orderMatcher);
        } else if (ordersListMatcher.matches()) {
            // Обрабатываем команду /orders - список всех заказов
            log.info("Admin {} requested all orders list", chatId);
            response = orderHandler.handleAllOrders(chatId);
        } else if (promoMatcher.matches()) {
            response = handlePromoCommand(chatId, promoMatcher);
        } else if (productMatcher.matches()) {
            response = handleProductCommand(chatId, productMatcher);
        } else if (productSearchMatcher.matches()) {
            response = handleProductSearchCommand(chatId, productSearchMatcher);
        } else if (productPriceMatcher.matches()) {
            response = handleProductPriceCommand(chatId, productPriceMatcher);
        } else if (productStockMatcher.matches()) {
            response = handleProductStockCommand(chatId, productStockMatcher);
        } else if (productCreateMatcher.matches()) {
            response = handleProductCreateCommand(chatId, productCreateMatcher);
        } else if (orderSearchMatcher.matches()) {
            response = handleOrderSearchCommand(chatId, orderSearchMatcher);
        } else if (promoCreateMatcher.matches()) {
            response = handlePromoCreateCommand(chatId, promoCreateMatcher);
        } else if (promoEditMatcher.matches()) {
            response = handlePromoEditCommand(chatId, promoEditMatcher);
        } else if (userSearchMatcher.find() || userSearchAltMatcher.find()) {
            String query = userSearchMatcher.find() ? userSearchMatcher.group(1) : userSearchAltMatcher.group(1);
            handleUserSearchCommand(chatId, message, "all", query);
        } else if (userNameSearchMatcher.find()) {
            handleUserSearchCommand(chatId, message, "name", userNameSearchMatcher.group(1));
        } else if (userSearchNameMatcher.find()) {
            handleUserSearchCommand(chatId, message, "name", userSearchNameMatcher.group(1));
        } else if (userEmailSearchMatcher.find()) {
            handleUserSearchCommand(chatId, message, "email", userEmailSearchMatcher.group(1));
        } else if (userPhoneSearchMatcher.find()) {
            handleUserSearchCommand(chatId, message, "phone", userPhoneSearchMatcher.group(1));
        } else if (userMatcher.find()) {
            handleUserDetailCommand(chatId, userMatcher);
        } else {
            // Обрабатываем стандартные команды
            response = handleStandardCommand(chatId, text);
        }
        
        if (response != null) {
            executeMethod(response);
        }
    }

    private BotApiMethod<?> handleOrderCommand(String chatId, Matcher orderMatcher) {
        try {
            Long orderId = Long.parseLong(orderMatcher.group(1));
            log.info("Admin {} requested order details: {}", chatId, orderId);
            return orderHandler.handleOrderDetails(chatId, orderId);
        } catch (Exception e) {
            log.error("Ошибка при обработке команды заказа: {}", e.getMessage(), e);
            return createMessage(chatId, "Ошибка при обработке команды заказа: " + e.getMessage());
        }
    }

    private BotApiMethod<?> handlePromoCommand(String chatId, Matcher promoMatcher) {
        if (promoMatcher.group(1) != null && !promoMatcher.group(1).isEmpty()) {
            Long promoId = Long.parseLong(promoMatcher.group(1));
            log.info("Admin {} requested promo code details: {}", chatId, promoId);
            return promoCodeHandler.handlePromoCodeDetails(chatId, promoId);
        } else {
            log.info("Admin {} requested all promo codes", chatId);
            return promoCodeHandler.handleAllPromoCodes(chatId);
        }
    }

    private BotApiMethod<?> handleProductCommand(String chatId, Matcher productMatcher) {
        Long productId = Long.parseLong(productMatcher.group(1));
        log.info("Admin {} requested product details: {}", chatId, productId);
        return productHandler.handleProductDetails(chatId, productId);
    }

    private BotApiMethod<?> handleProductSearchCommand(String chatId, Matcher productSearchMatcher) {
        String query = productSearchMatcher.group(1);
        log.info("Admin {} requested product search: {}", chatId, query);
        return productHandler.handleProductSearch(chatId, query);
    }

    private BotApiMethod<?> handleProductPriceCommand(String chatId, Matcher productPriceMatcher) {
        Long productId = Long.parseLong(productPriceMatcher.group(1));
        String price = productPriceMatcher.group(2);
        log.info("Admin {} requested product price update: {} to {}", chatId, productId, price);
        return productHandler.handleUpdatePrice(chatId, productId, price);
    }

    private BotApiMethod<?> handleProductStockCommand(String chatId, Matcher productStockMatcher) {
        Long productId = Long.parseLong(productStockMatcher.group(1));
        String stock = productStockMatcher.group(2);
        log.info("Admin {} requested product stock update: {} to {}", chatId, productId, stock);
        return productHandler.handleUpdateStock(chatId, productId, stock);
    }

    private BotApiMethod<?> handleProductCreateCommand(String chatId, Matcher productCreateMatcher) {
        String productData = productCreateMatcher.group(1);
        log.info("Admin {} requested product creation: {}", chatId, productData);
        return productHandler.handleCreateProduct(chatId, productData);
    }

    private BotApiMethod<?> handleOrderSearchCommand(String chatId, Matcher orderSearchMatcher) {
        if (orderSearchMatcher.find()) {
            String query = orderSearchMatcher.group(1);
            log.info("Admin {} requested order search: {}", chatId, query);
            return orderHandler.handleOrderSearch(chatId, query);
        }
        log.info("Admin {} requested order search form", chatId);
        return orderHandler.handleOrderSearchRequest(chatId);
    }

    private BotApiMethod<?> handlePromoCreateCommand(String chatId, Matcher promoCreateMatcher) {
        String promoData = promoCreateMatcher.group(0).substring("/promo_create ".length());
        log.info("Admin {} requested promo code creation: {}", chatId, promoData);
        return promoCodeHandler.handleCreatePromoCode(chatId, promoData);
    }

    private BotApiMethod<?> handlePromoEditCommand(String chatId, Matcher promoEditMatcher) {
        if (promoEditMatcher.find()) {
            Long promoId = Long.parseLong(promoEditMatcher.group(1));
            String code = promoEditMatcher.group(2);
            int discount = Integer.parseInt(promoEditMatcher.group(3));
            int maxUses = Integer.parseInt(promoEditMatcher.group(4));
            String description = promoEditMatcher.group(5);
            log.info("Admin {} requested promo code update: {}", chatId, promoId);
            return promoCodeHandler.handleUpdatePromoCode(chatId, promoId, code, discount, maxUses, description);
        }
        return null;
    }

    private void handleUserDetailCommand(String chatId, Matcher userMatcher) {
        if (!userMatcher.group(1).isEmpty()) {
            Long userId = Long.parseLong(userMatcher.group(1));
            log.info("Admin {} requested user details: {}", chatId, userId);
            SendMessage sendMessage = userHandler.handleUserDetails(chatId, userId);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки сообщения: {}", e.getMessage());
            }
        } else {
            log.info("Admin {} requested users list", chatId);
            SendMessage sendMessage = userHandler.handleUserList(chatId);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Ошибка отправки сообщения: {}", e.getMessage());
            }
        }
    }

    private BotApiMethod<?> handleStandardCommand(String chatId, String text) {
        log.info("Admin {} sent command: {}", chatId, text);
        
        // Проверяем состояние пользователя
        UserState currentState = getUserState(chatId);
        if (currentState == UserState.WAITING_ORDER_SEARCH) {
            // Если пользователь в состоянии поиска заказа, обрабатываем как поиск
            return orderHandler.handleOrderSearch(chatId, text);
        }
        
        return switch (text) {
            case "/start", "/help" -> createWelcomeMessage(chatId);
            case "📋 Все заказы" -> orderHandler.handleAllOrders(chatId);
            case "/stats", "📊 Статистика" -> orderHandler.handleOrderStatistics(chatId);
            case "/users", "👤 Пользователи" -> userHandler.handleUserList(chatId);
            case "/menu" -> createMainMenuMessage(chatId);
            case "🔍 Поиск заказа" -> {
                setUserState(chatId, UserState.WAITING_ORDER_SEARCH);
                yield orderHandler.handleOrderSearchRequest(chatId);
            }
            case "🎨 NFT" -> createNFTMenuMessage(chatId);
            case "⚙️ Настройки" -> createSettingsMessage(chatId);
            case "/promo", "🔖 Промокоды" -> promoCodeHandler.handleAllPromoCodes(chatId);
            case "/products", "👕 Товары" -> productHandler.handleAllProducts(chatId);
            case "💰 Пополнения" -> {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("💰 *Управление пополнениями*\n\nВыберите фильтр:");
                message.setParseMode("Markdown");
                message.setReplyMarkup(AdminKeyboards.createDepositsKeyboard());
                yield message;
            }
            default -> createUnknownCommandMessage(chatId);
        };
    }

    /**
     * Обработка callback-запросов (нажатие inline-кнопок)
     */
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();
        String callbackId = callbackQuery.getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("Обработка callback-запроса: {}", data);

        try {
            if (data.startsWith("menu:")) {
                handleMenuCallback(chatId, data, messageId);
            } else if (data.startsWith("filter:")) {
                handleFilterCallback(chatId, data, messageId);
            } else if (data.startsWith("promo:")) {
                handlePromoCallback(chatId, data, messageId);
            } else if (data.startsWith("deposit_confirm_")) {
                handleDepositConfirmCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposit_reject_")) {
                handleDepositRejectCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposit_details_")) {
                handleDepositDetailsCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposits:")) {
                BotApiMethod<?> response = handleDepositsFilterCallback(chatId, callbackId, data, messageId);
                if (response != null) {
                    executeMethod(response);
                }
            } else if (data.equals("listUsers")) {
                executeMethod(userHandler.handleListUsers(chatId));
            } else if (data.equals("searchUser")) {
                executeMethod(userHandler.handleSearchUser(chatId));
            } else if (data.equals("searchUserByName")) {
                handleUserSearchByName(chatId);
            } else if (data.equals("searchUserByEmail")) {
                executeMethod(userHandler.handleSearchUserByEmail(chatId));
            } else if (data.equals("searchUserByPhone")) {
                executeMethod(userHandler.handleSearchUserByPhone(chatId));
            }
            
            // Отправляем пустой ответ на callback, чтобы убрать "часики" на кнопке
            sendCallbackAnswer(callbackId, "", false);
        } catch (Exception e) {
            log.error("Ошибка при обработке callback-запроса: {}", e.getMessage(), e);
            sendCallbackAnswer(callbackId, "❌ Произошла ошибка: " + e.getMessage(), true);
        }
    }

    private void handleFilterCallback(String chatId, String data, Integer messageId) {
        String filter = data.substring("filter:".length());
        switch (filter) {
            case "all":
                executeMethod(orderHandler.handleAllOrders(chatId));
                break;
            case "today":
                executeMethod(orderHandler.handleTodayOrders(chatId));
                break;
            case "week":
                executeMethod(orderHandler.handleWeekOrders(chatId));
                break;
            case "month":
                executeMethod(orderHandler.handleMonthOrders(chatId));
                break;
            case "search":
                setUserState(chatId, UserState.WAITING_ORDER_SEARCH);
                executeMethod(orderHandler.handleOrderSearchRequest(chatId));
                break;
            case "NEW":
                executeMethod(orderHandler.handleOrdersByStatus(chatId, OrderStatus.NEW));
                break;
            case "PROCESSING":
                executeMethod(orderHandler.handleOrdersByStatus(chatId, OrderStatus.PROCESSING));
                break;
            case "DISPATCHED":
                executeMethod(orderHandler.handleOrdersByStatus(chatId, OrderStatus.DISPATCHED));
                break;
            case "COMPLETED":
                executeMethod(orderHandler.handleOrdersByStatus(chatId, OrderStatus.COMPLETED));
                break;
            case "CANCELLED":
                executeMethod(orderHandler.handleOrdersByStatus(chatId, OrderStatus.CANCELLED));
                break;
        }
    }

    private void handlePromoCallback(String chatId, String data, Integer messageId) {
        String action = data.substring("promo:".length());
        switch (action) {
            case "active":
                executeMethod(promoCodeHandler.handleActivePromoCodes(chatId));
                break;
            case "expired":
                executeMethod(promoCodeHandler.handleExpiredPromoCodes(chatId));
                break;
            case "create":
                executeMethod(promoCodeHandler.handleCreatePromoCode(chatId));
                break;
        }
    }

    private void handleMenuCallback(String chatId, String data, Integer messageId) {
        String menu = data.substring("menu:".length());
        
        switch (menu) {
            case "main":
                showMainMenu(chatId);
                break;
            case "orders":
                showOrdersMenu(chatId);
                break;
            case "users":
                showUsersMenu(chatId);
                break;
            case "promocodes":
                showPromoCodesMenu(chatId);
                break;
            case "products":
                showProductsMenu(chatId);
                break;
            case "deposits":
                handleDepositsFilterCallback(chatId, null, "deposits:all", messageId);
                break;
            case "search":
                setUserState(chatId, UserState.WAITING_ORDER_SEARCH);
                executeMethod(orderHandler.handleOrderSearchRequest(chatId));
                break;
            case "settings":
                showSettingsMenu(chatId);
                break;
            default:
                showMainMenu(chatId);
        }
    }

    private void showMainMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 *Главное меню*\n\nВыберите раздел:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createMainMenu());
        executeMethod(message);
    }

    private void showOrdersMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📦 *Управление заказами*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createOrderFiltersKeyboard());
        executeMethod(message);
    }

    private void showUsersMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👥 *Управление пользователями*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createUsersMenu());
        executeMethod(message);
    }

    private void showPromoCodesMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎟 *Управление промокодами*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createPromoCodesKeyboard());
        executeMethod(message);
    }

    private void showProductsMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🛍 *Управление товарами*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createProductsKeyboard());
        executeMethod(message);
    }

    private void showSettingsMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚙️ *Настройки*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createSettingsKeyboard());
        executeMethod(message);
    }

    /**
     * Обрабатывает запрос на подтверждение пополнения
     */
    @Transactional
    private void handleDepositConfirmCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на подтверждение пополнения: {}", data);
        try {
            Long transactionId = Long.parseLong(data.substring("deposit_confirm_".length()));
            
            // Проверяем статус транзакции перед подтверждением
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isPresent() && transactionOpt.get().getStatus() != TransactionStatus.PENDING) {
                log.warn("Попытка подтвердить транзакцию в статусе {}: {}", 
                        transactionOpt.get().getStatus(), transactionOpt.get().getTransactionCode());
                sendCallbackAnswer(callbackId, "❌ Невозможно подтвердить транзакцию в статусе " + transactionOpt.get().getStatus(), true);
                return;
            }
            
            log.info("Вызов метода подтверждения для транзакции ID: {}", transactionId);
            BotApiMethod<?> response = handleDepositConfirm(chatId, transactionId);
            
            if (response != null) {
                log.info("Отправка ответного сообщения пользователю");
                executeMethod(response);
                sendCallbackAnswer(callbackId, "Транзакция подтверждена", false);
            } else {
                log.warn("Метод handleDepositConfirm вернул null для транзакции ID: {}", transactionId);
                sendCallbackAnswer(callbackId, "Ошибка при обработке запроса", true);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке callback-запроса на подтверждение: {}", e.getMessage(), e);
            sendCallbackAnswer(callbackId, "Ошибка: " + e.getMessage(), true);
            
            try {
                // Отправим сообщение об ошибке
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ Ошибка при подтверждении транзакции: " + e.getMessage());
                execute(errorMessage);
            } catch (Exception ex) {
                log.error("Не удалось отправить сообщение об ошибке: {}", ex.getMessage());
            }
        } finally {
            log.info("<< Завершение обработки callback-запроса на подтверждение: {}", data);
        }
    }

    /**
     * Обрабатывает запрос на отклонение пополнения
     */
    @Transactional
    private void handleDepositRejectCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на отклонение пополнения: {}", data);
        try {
            Long transactionId = Long.parseLong(data.substring("deposit_reject_".length()));
            
            // Проверяем статус транзакции перед отклонением
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isPresent() && transactionOpt.get().getStatus() != TransactionStatus.PENDING) {
                log.warn("Попытка отклонить транзакцию в статусе {}: {}", 
                        transactionOpt.get().getStatus(), transactionOpt.get().getTransactionCode());
                sendCallbackAnswer(callbackId, "❌ Невозможно отклонить транзакцию в статусе " + transactionOpt.get().getStatus(), true);
                return;
            }
            
            log.info("Вызов метода отклонения для транзакции ID: {}", transactionId);
            BotApiMethod<?> response = handleDepositReject(chatId, transactionId);
            
            if (response != null) {
                log.info("Отправка ответного сообщения пользователю");
                executeMethod(response);
                sendCallbackAnswer(callbackId, "Транзакция отклонена", false);
            } else {
                log.warn("Метод handleDepositReject вернул null для транзакции ID: {}", transactionId);
                sendCallbackAnswer(callbackId, "Ошибка при обработке запроса", true);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке callback-запроса на отклонение: {}", e.getMessage(), e);
            sendCallbackAnswer(callbackId, "Ошибка: " + e.getMessage(), true);
            
            try {
                // Отправим сообщение об ошибке
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ Ошибка при отклонении транзакции: " + e.getMessage());
                execute(errorMessage);
            } catch (Exception ex) {
                log.error("Не удалось отправить сообщение об ошибке: {}", ex.getMessage());
            }
        } finally {
            log.info("<< Завершение обработки callback-запроса на отклонение: {}", data);
        }
    }

    /**
     * Обрабатывает запрос на просмотр деталей пополнения
     */
    @Transactional
    private void handleDepositDetailsCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на просмотр деталей пополнения: {}", data);
        try {
            Long transactionId = Long.parseLong(data.substring("deposit_details_".length()));
            
            log.info("Вызов метода просмотра деталей для транзакции ID: {}", transactionId);
            BotApiMethod<?> response = handleDepositDetails(chatId, transactionId);
            
            if (response != null) {
                log.info("Отправка ответного сообщения пользователю");
                executeMethod(response);
                sendCallbackAnswer(callbackId, "Детали транзакции", false);
            } else {
                log.warn("Метод handleDepositDetails вернул null для транзакции ID: {}", transactionId);
                sendCallbackAnswer(callbackId, "Ошибка при обработке запроса", true);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке callback-запроса на просмотр деталей: {}", e.getMessage(), e);
            sendCallbackAnswer(callbackId, "Ошибка: " + e.getMessage(), true);
            
            try {
                // Отправим сообщение об ошибке
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId);
                errorMessage.setText("❌ Ошибка при просмотре деталей транзакции: " + e.getMessage());
                execute(errorMessage);
            } catch (Exception ex) {
                log.error("Не удалось отправить сообщение об ошибке: {}", ex.getMessage());
            }
        } finally {
            log.info("<< Завершение обработки callback-запроса на просмотр деталей: {}", data);
        }
    }

    /**
     * Обрабатывает запрос на подтверждение пополнения
     */
    @Transactional
    protected BotApiMethod<?> handleDepositConfirm(String chatId, Long transactionId) {
        log.info(">> Начинаем обработку запроса на подтверждение транзакции ID: {}", transactionId);
        try {
            // Используем метод с жадной загрузкой пользователя
            Optional<Transaction> transactionOpt = transactionRepository.findByIdWithUser(transactionId);
            if (transactionOpt.isEmpty()) {
                log.warn("Транзакция с ID {} не найдена в базе данных", transactionId);
                return createMessage(chatId, "❌ Транзакция не найдена.");
            }
            
            Transaction transaction = transactionOpt.get();
            // Теперь мы можем безопасно получить пользователя, так как он загружен жадно
            String username = transaction.getUser().getUsername();
            log.info("Найдена транзакция: ID={}, статус={}, пользователь={}, сумма={}", 
                    transaction.getId(), transaction.getStatus(), username, transaction.getAmount());
            
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                log.warn("Невозможно подтвердить транзакцию в статусе {}: {}", 
                       transaction.getStatus(), transaction.getTransactionCode());
                return createMessage(chatId, "❌ Невозможно подтвердить транзакцию в статусе " + transaction.getStatus());
            }
            
            log.info("Вызываем метод подтверждения транзакции в BalanceService");
            // Подтверждаем транзакцию через сервис
            Transaction confirmedTransaction = balanceService.confirmDeposit(transaction.getTransactionCode(), "Admin: " + chatId);
            
            log.info("Транзакция успешно подтверждена: ID={}, новый статус={}", 
                    confirmedTransaction.getId(), confirmedTransaction.getStatus());
            
            return createMessage(chatId, "✅ Транзакция #" + transactionId + " успешно подтверждена. Баланс пользователя пополнен.");
        } catch (Exception e) {
            log.error("Исключение при подтверждении транзакции {}: {}", transactionId, e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на подтверждение транзакции ID: {}", transactionId);
        }
    }
    
    /**
     * Обрабатывает запрос на отклонение пополнения
     */
    @Transactional
    protected BotApiMethod<?> handleDepositReject(String chatId, Long transactionId) {
        log.info(">> Начинаем обработку запроса на отклонение транзакции ID: {}", transactionId);
        try {
            // Используем метод с жадной загрузкой пользователя
            Optional<Transaction> transactionOpt = transactionRepository.findByIdWithUser(transactionId);
            if (transactionOpt.isEmpty()) {
                log.warn("Транзакция с ID {} не найдена в базе данных", transactionId);
                return createMessage(chatId, "❌ Транзакция не найдена.");
            }
            
            Transaction transaction = transactionOpt.get();
            // Теперь мы можем безопасно получить пользователя, так как он загружен жадно
            String username = transaction.getUser().getUsername();
            log.info("Найдена транзакция: ID={}, статус={}, пользователь={}, сумма={}", 
                    transaction.getId(), transaction.getStatus(), username, transaction.getAmount());
            
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                log.warn("Невозможно отклонить транзакцию в статусе {}: {}", 
                       transaction.getStatus(), transaction.getTransactionCode());
                return createMessage(chatId, "❌ Невозможно отклонить транзакцию в статусе " + transaction.getStatus());
            }
            
            log.info("Вызываем метод отклонения транзакции в BalanceService");
            // Отклоняем транзакцию через сервис
            Transaction rejectedTransaction = balanceService.rejectDeposit(
                transaction.getTransactionCode(), 
                "Отклонено администратором", 
                "Admin: " + chatId
            );
            
            log.info("Транзакция успешно отклонена: ID={}, новый статус={}", 
                    rejectedTransaction.getId(), rejectedTransaction.getStatus());
            
            return createMessage(chatId, "❌ Транзакция #" + transactionId + " отклонена.");
        } catch (Exception e) {
            log.error("Исключение при отклонении транзакции {}: {}", transactionId, e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на отклонение транзакции ID: {}", transactionId);
        }
    }
    
    /**
     * Форматирует дату и время в читаемый вид
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }
    
    /**
     * Возвращает эмодзи для статуса транзакции
     */
    private String getStatusEmoji(TransactionStatus status) {
        String emoji;
        switch (status) {
            case PENDING:
                emoji = "⏳";
                break;
            case COMPLETED:
                emoji = "✅";
                break;
            case REJECTED:
                emoji = "❌";
                break;
            case CANCELLED:
                emoji = "🚫";
                break;
            default:
                emoji = "";
                break;
        }
        return emoji;
    }

    /**
     * Создает приветственное сообщение
     */
    private SendMessage createWelcomeMessage(String chatId) {
        String text = """
                *🤖 Административный бот Clothing Brand*
                
                Добро пожаловать в административную панель!
                
                Используйте кнопки ниже для управления заказами, просмотра статистики и управления пользователями.
                
                */menu* - вызов главного меню
                */orders* - список всех заказов
                */stats* - статистика заказов
                */users* - список пользователей
                */promo* - управление промокодами
                */products* - управление товарами
                */help* - эта справка
                """;
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createMainMenu());
        return message;
    }

    /**
     * Создает главное меню
     */
    private SendMessage createMainMenuMessage(String chatId) {
        String text = "*📋 Главное меню*\n\nВыберите раздел:";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createMainMenu());
        return message;
    }

    /**
     * Создает сообщение для поиска заказов
     */
    private SendMessage createSearchMessage(String chatId) {
        String text = """
                *🔍 Поиск заказа*
                
                Введите номер заказа, email или телефон клиента.
                
                Например: /search #ORD-12345678
                Или: /search email@example.com
                Или: /search +79123456789
                """;
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }

    /**
     * Создает меню NFT
     */
    private SendMessage createNFTMenuMessage(String chatId) {
        String text = "*🎨 Управление NFT*\n\nВыберите опцию:";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createNFTKeyboard());
        return message;
    }

    /**
     * Создает список всех NFT
     */
    private SendMessage createAllNFTsMessage(String chatId) {
        // Заглушка, будет реализовано позже
        String text = "*🎨 Все NFT*\n\nСписок всех NFT будет доступен в следующей версии.";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createBackKeyboard("nft:menu"));
        return message;
    }

    /**
     * Создает список нераскрытых NFT
     */
    private SendMessage createUnrevealedNFTsMessage(String chatId) {
        // Заглушка, будет реализовано позже
        String text = "*🎁 Нераскрытые NFT*\n\nСписок нераскрытых NFT будет доступен в следующей версии.";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createBackKeyboard("nft:menu"));
        return message;
    }

    /**
     * Создает форму поиска NFT по пользователю
     */
    private SendMessage createNFTSearchMessage(String chatId) {
        // Заглушка, будет реализовано позже
        String text = "*🔍 Поиск NFT по владельцу*\n\nВведите имя пользователя для поиска NFT.";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createBackKeyboard("nft:menu"));
        return message;
    }

    /**
     * Создает раздел настроек
     */
    private SendMessage createSettingsMessage(String chatId) {
        String text = "*⚙️ Настройки*\n\nРаздел настроек будет доступен в следующей версии.";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }

    /**
     * Создает сообщение о топ продуктах
     */
    private SendMessage createTopProductsMessage(String chatId) {
        // Заглушка, будет реализована позже
        String text = "*🔝 Популярные товары*\n\nСтатистика по популярным товарам будет доступна в следующей версии.";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createBackKeyboard("stats:general"));
        return message;
    }

    /**
     * Создает сообщение для неизвестной команды
     */
    private SendMessage createUnknownCommandMessage(String chatId) {
        String text = "Неизвестная команда. Используйте /help для получения списка доступных команд.";
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }

    /**
     * Отправляет простое текстовое сообщение
     */
    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    private boolean isAdmin(String chatId) {
        boolean isAdminUser = allowedAdminIds.contains(chatId);
        log.debug("Проверка доступа для ID {}: {}", chatId, isAdminUser ? "разрешен" : "запрещен");
        if (!isAdminUser) {
            log.warn("Попытка доступа с неразрешенного ID: {}. Список разрешенных ID: {}", chatId, allowedAdminIds);
        }
        return isAdminUser;
    }

    /**
     * Создаёт простое текстовое сообщение
     */
    private SendMessage createMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");
        return message;
    }

    /**
     * Отправляет ответ на callback-запрос
     */
    private void sendCallbackAnswer(String callbackId, String text, boolean isError) {
        log.info("Отправка ответа на callback {}: {}, isError: {}", callbackId, text, isError);
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(text);
        answer.setShowAlert(isError);
        try {
            execute(answer);
            log.info("Ответ на callback успешно отправлен");
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа на callback: {}", e.getMessage(), e);
        }
    }

    /**
     * Выполняет метод API бота
     */
    private void executeMethod(BotApiMethod<?> method) {
        if (method == null) {
            log.warn("Попытка выполнить null-метод API бота");
            return;
        }
        
        try {
            log.info("Выполнение метода API бота: {}", method.getClass().getSimpleName());
            execute(method);
            log.info("Метод API бота успешно выполнен");
        } catch (TelegramApiException e) {
            log.error("Ошибка выполнения метода API бота: {}", e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает запрос на просмотр NFT пользователя
     */
    private SendMessage handleUserNFTs(String chatId, Long userId) {
        User user = adminBotService.getUserById(userId);
        
        if (user == null) {
            return createMessage(chatId, "Пользователь не найден.");
        }
        
        List<NFT> userNFTs = adminBotService.getNFTsByUser(user);
        
        if (userNFTs.isEmpty()) {
            return createMessage(chatId, "У пользователя " + user.getUsername() + " нет NFT.");
        }
        
        StringBuilder message = new StringBuilder("🎨 NFT пользователя " + user.getUsername() + ":\n\n");
        
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
        
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(createButton("◀️ Назад к пользователю", "viewUser:" + userId));
        rows.add(row);
        
        keyboard.setKeyboard(rows);
        
        return createMessage(chatId, message.toString(), keyboard);
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
     * Создаёт объект сообщения с клавиатурой
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        SendMessage message = createMessage(chatId, text);
        message.setReplyMarkup(keyboard);
        return message;
    }

    /**
     * Обрабатывает запрос поиска пользователя по имени
     *
     * @param chatId ID чата
     */
    private void handleUserSearchByName(String chatId) {
        log.info("Запрос на поиск пользователя по имени, chatId: {}", chatId);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔍 *Поиск пользователя по имени*\n\n" +
                        "Введите или скопируйте команду ниже и добавьте имя пользователя:\n\n" +
                        "`/search_user имя_пользователя`\n\n" +
                        "💡 Команду можно скопировать, нажав на неё.");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    /**
     * Обрабатывает запрос поиска пользователя по email
     *
     * @param chatId ID чата
     */
    private void handleUserSearchByEmail(String chatId) {
        log.info("Запрос на поиск пользователя по email, chatId: {}", chatId);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📧 *Поиск пользователя по email*\n\n" +
                        "Введите или скопируйте команду ниже и добавьте email:\n\n" +
                        "`/email адрес@почты.com`\n\n" +
                        "💡 Команду можно скопировать, нажав на неё.");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    /**
     * Обрабатывает запрос поиска пользователя по телефону
     *
     * @param chatId ID чата
     */
    private void handleUserSearchByPhone(String chatId) {
        log.info("Запрос на поиск пользователя по телефону, chatId: {}", chatId);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📱 *Поиск пользователя по телефону*\n\n" +
                        "Введите или скопируйте команду ниже и добавьте номер телефона:\n\n" +
                        "`/phone +79991234567`\n\n" +
                        "💡 Команду можно скопировать, нажав на неё.");
        message.setParseMode("Markdown");
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void createUsersMenuMessage(String chatId) {
        log.info("Создание меню управления пользователями, chatId: {}", chatId);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👥 *Управление пользователями*\n\nВыберите действие из меню ниже:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createUsersMenu());
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    private void handleUserSearchCommand(String chatId, Message message, String searchType, String query) {
        log.info("Админ {} запросил поиск пользователей по {}: {}", chatId, searchType, query);
        
        String searchTypeEmoji = switch(searchType) {
            case "name" -> "👤";
            case "email" -> "📧";
            case "phone" -> "📱";
            default -> "🔍";
        };
        
        SendMessage sendMessage = userHandler.handleUserSearch(chatId, query, searchType);
        
        // Добавим информацию о поиске в текст сообщения
        String originalText = sendMessage.getText();
        String searchInfo = searchTypeEmoji + " *Результаты поиска пользователей*" +
                           "\nКритерий: " + switch(searchType) {
                               case "name" -> "имя";
                               case "email" -> "email";
                               case "phone" -> "телефон";
                               default -> "все поля";
                           } +
                           "\nЗапрос: `" + query + "`\n\n";
        
        sendMessage.setText(searchInfo + originalText);
        sendMessage.setParseMode("Markdown");
        
        try {
            execute(sendMessage);
            log.info("Результаты поиска пользователей успешно отправлены");
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    /**
     * Возвращает текущее состояние пользователя
     */
    private UserState getUserState(String chatId) {
        return userStates.getOrDefault(chatId, UserState.NONE);
    }
    
    /**
     * Устанавливает состояние пользователя
     */
    private void setUserState(String chatId, UserState state) {
        log.info("Установка состояния для админа {}: {}", chatId, state);
        if (state == UserState.NONE) {
            userStates.remove(chatId);
        } else {
            userStates.put(chatId, state);
        }
    }
    
    /**
     * Возвращает список разрешенных ID администраторов
     */
    public Set<String> getAllowedAdminIds() {
        return Collections.unmodifiableSet(allowedAdminIds);
    }

    /**
     * Обрабатывает запрос на просмотр деталей пополнения
     */
    @Transactional
    protected BotApiMethod<?> handleDepositDetails(String chatId, Long transactionId) {
        try {
            // Используем метод с жадной загрузкой пользователя
            Optional<Transaction> transactionOpt = transactionRepository.findByIdWithUser(transactionId);
            if (transactionOpt.isEmpty()) {
                return createMessage(chatId, "❌ Транзакция не найдена.");
            }

            Transaction transaction = transactionOpt.get();
            // Теперь мы можем безопасно получить данные пользователя, так как он загружен жадно
            String username = transaction.getUser().getUsername();
            String email = transaction.getUser().getEmail();
            
            StringBuilder message = new StringBuilder();
            message.append("*📋 ДЕТАЛИ ПОПОЛНЕНИЯ #").append(transactionId).append("*\n\n");
            message.append("*Пользователь:* ").append(username).append("\n");
            message.append("*Email:* ").append(email != null ? email : "-").append("\n");
            message.append("*ID пользователя:* ").append(transaction.getUser().getId()).append("\n");
            message.append("*Сумма:* ").append(transaction.getAmount()).append(" ₽\n");
            message.append("*Статус:* ").append(getStatusEmoji(transaction.getStatus())).append(" ").append(transaction.getStatus()).append("\n");
            message.append("*Код транзакции:* `").append(transaction.getTransactionCode()).append("`\n");
            message.append("*Тип:* ").append(transaction.getType()).append("\n");
            message.append("*Создана:* ").append(formatDateTime(transaction.getCreatedAt())).append("\n");
            
            if (transaction.getUpdatedAt() != null) {
                message.append("*Обновлена:* ").append(formatDateTime(transaction.getUpdatedAt())).append("\n");
            }
            
            if (transaction.getAdminComment() != null && !transaction.getAdminComment().isEmpty()) {
                message.append("*Комментарий админа:* ").append(transaction.getAdminComment()).append("\n");
            }

            // Создаем клавиатуру с кнопками действий
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            // Кнопки подтверждения/отклонения в зависимости от статуса
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                
                InlineKeyboardButton confirmButton = new InlineKeyboardButton();
                confirmButton.setText("✅ Подтвердить");
                confirmButton.setCallbackData("deposit_confirm_" + transactionId);
                row.add(confirmButton);
                
                InlineKeyboardButton rejectButton = new InlineKeyboardButton();
                rejectButton.setText("❌ Отклонить");
                rejectButton.setCallbackData("deposit_reject_" + transactionId);
                row.add(rejectButton);
                
                keyboard.add(row);
            }
            
            // Кнопка возврата к меню
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀️ Назад к меню");
            backButton.setCallbackData("menu:main");
            row2.add(backButton);
            keyboard.add(row2);
            
            keyboardMarkup.setKeyboard(keyboard);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message.toString());
            sendMessage.setParseMode("Markdown");
            sendMessage.setReplyMarkup(keyboardMarkup);
            
            return sendMessage;
        } catch (Exception e) {
            log.error("Ошибка при получении деталей транзакции: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает запрос на фильтрацию пополнений
     */
    private BotApiMethod<?> handleDepositsFilterCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на фильтрацию пополнений: {}", data);
        try {
            String filter = data.substring("deposits:".length());
            switch (filter) {
                case "all":
                    return handleAllDeposits(chatId);
                case "pending":
                    return handlePendingDeposits(chatId);
                case "completed":
                    return handleCompletedDeposits(chatId);
                case "rejected":
                    return handleRejectedDeposits(chatId);
                default:
                    return createMessage(chatId, "❌ Неизвестный фильтр: " + filter);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке фильтра пополнений: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки callback-запроса на фильтрацию пополнений: {}", data);
        }
    }

    /**
     * Обрабатывает запрос на просмотр всех пополнений
     */
    private BotApiMethod<?> handleAllDeposits(String chatId) {
        log.info(">> Обработка запроса на просмотр всех пополнений");
        try {
            List<Transaction> transactions = transactionRepository.findAll();
            return createDepositsListMessage(chatId, transactions, "Все пополнения");
        } catch (Exception e) {
            log.error("Ошибка при получении списка пополнений: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на просмотр всех пополнений");
        }
    }

    /**
     * Обрабатывает запрос на просмотр ожидающих пополнений
     */
    private BotApiMethod<?> handlePendingDeposits(String chatId) {
        log.info(">> Обработка запроса на просмотр ожидающих пополнений");
        try {
            List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
            return createDepositsListMessage(chatId, transactions, "Ожидающие пополнения");
        } catch (Exception e) {
            log.error("Ошибка при получении списка ожидающих пополнений: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на просмотр ожидающих пополнений");
        }
    }

    /**
     * Обрабатывает запрос на просмотр завершенных пополнений
     */
    private BotApiMethod<?> handleCompletedDeposits(String chatId) {
        log.info(">> Обработка запроса на просмотр завершенных пополнений");
        try {
            List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.COMPLETED);
            return createDepositsListMessage(chatId, transactions, "Завершенные пополнения");
        } catch (Exception e) {
            log.error("Ошибка при получении списка завершенных пополнений: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на просмотр завершенных пополнений");
        }
    }

    /**
     * Обрабатывает запрос на просмотр отклоненных пополнений
     */
    private BotApiMethod<?> handleRejectedDeposits(String chatId) {
        log.info(">> Обработка запроса на просмотр отклоненных пополнений");
        try {
            List<Transaction> transactions = transactionRepository.findByStatus(TransactionStatus.REJECTED);
            return createDepositsListMessage(chatId, transactions, "Отклоненные пополнения");
        } catch (Exception e) {
            log.error("Ошибка при получении списка отклоненных пополнений: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
        } finally {
            log.info("<< Завершение обработки запроса на просмотр отклоненных пополнений");
        }
    }

    /**
     * Создает сообщение со списком пополнений
     */
    private BotApiMethod<?> createDepositsListMessage(String chatId, List<Transaction> transactions, String title) {
        if (transactions.isEmpty()) {
            return createMessage(chatId, "📋 *" + title + "*\n\nНет пополнений для отображения.");
        }

        StringBuilder message = new StringBuilder();
        message.append("📋 *").append(title).append("*\n\n");

        for (Transaction transaction : transactions) {
            message.append("ID: ").append(transaction.getId()).append("\n");
            message.append("Пользователь: ").append(transaction.getUser().getUsername()).append("\n");
            message.append("Сумма: ").append(transaction.getAmount()).append(" ₽\n");
            message.append("Статус: ").append(getStatusEmoji(transaction.getStatus())).append(" ").append(transaction.getStatus()).append("\n");
            message.append("Дата: ").append(formatDateTime(transaction.getCreatedAt())).append("\n");
            message.append("Код: `").append(transaction.getTransactionCode()).append("`\n");
            message.append("-------------------\n");
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Кнопки фильтров
        List<InlineKeyboardButton> filterRow = new ArrayList<>();
        filterRow.add(createButton("Все", "deposits:all"));
        filterRow.add(createButton("Ожидают", "deposits:pending"));
        filterRow.add(createButton("Завершены", "deposits:completed"));
        filterRow.add(createButton("Отклонены", "deposits:rejected"));
        keyboard.add(filterRow);

        // Кнопка возврата
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createButton("◀️ Назад", "menu:main"));
        keyboard.add(backRow);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message.toString());
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboardMarkup);

        return sendMessage;
    }
}
