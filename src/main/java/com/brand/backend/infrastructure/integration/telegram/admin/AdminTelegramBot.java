package com.brand.backend.infrastructure.integration.telegram.admin;

import com.brand.backend.infrastructure.integration.telegram.admin.handlers.OrderHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.UserHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.PromoCodeHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.ProductHandler;
import com.brand.backend.infrastructure.integration.telegram.admin.handlers.NFTHandler;
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
import com.brand.backend.application.payment.service.TransactionAdminService;
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
    private final NFTHandler nftHandler;
    private final TransactionRepository transactionRepository;
    private final BalanceService balanceService;
    private final TransactionAdminService transactionAdminService;
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
    
    /**
     * Возвращает множество идентификаторов администраторов
     */
    public Set<String> getAllowedAdminIds() {
        return Collections.unmodifiableSet(allowedAdminIds);
    }
    
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
    // Регулярное выражение для обработки команд вида /deposit123 или /deposit_123
    private static final Pattern DEPOSIT_COMMAND_PATTERN = Pattern.compile("/deposit(?:_)?(\\d+)");

    // Конструктор с инициализацией всех необходимых полей
    public AdminTelegramBot(
            OrderRepository orderRepository,
            OrderHandler orderHandler,
            UserHandler userHandler,
            AdminBotService adminBotService,
            PromoCodeHandler promoCodeHandler,
            ProductHandler productHandler,
            NFTHandler nftHandler,
            TransactionRepository transactionRepository,
            BalanceService balanceService,
            TransactionAdminService transactionAdminService,
            @Value("${admin.bot.token}") String botToken) {
        super(botToken); // Передаем токен в конструктор суперкласса
        this.orderRepository = orderRepository;
        this.orderHandler = orderHandler;
        this.userHandler = userHandler;
        this.adminBotService = adminBotService;
        this.promoCodeHandler = promoCodeHandler;
        this.productHandler = productHandler;
        this.nftHandler = nftHandler;
        this.transactionRepository = transactionRepository;
        this.balanceService = balanceService;
        this.transactionAdminService = transactionAdminService;
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
        Matcher nameSearchMatcher = USER_NAME_SEARCH_PATTERN.matcher(text);
        Matcher userSearchNameMatcher = USER_SEARCH_NAME_PATTERN.matcher(text);
        Matcher userEmailSearchMatcher = USER_EMAIL_SEARCH_PATTERN.matcher(text);
        Matcher userPhoneSearchMatcher = USER_PHONE_SEARCH_PATTERN.matcher(text);
        Matcher depositMatcher = DEPOSIT_COMMAND_PATTERN.matcher(text);
        
        if (orderMatcher.matches()) {
            response = handleOrderCommand(chatId, orderMatcher);
        } else if (ordersListMatcher.matches()) {
            // Обрабатываем команду /orders - список всех заказов
            log.info("Admin {} requested all orders list", chatId);
            response = orderHandler.handleAllOrders(chatId);
        } else if (depositMatcher.matches()) {
            response = handleDepositCommand(chatId, depositMatcher);
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
        } else if (nameSearchMatcher.matches()) {
            String name = nameSearchMatcher.group(1);
            log.info("Найдена команда поиска пользователя по имени: /name {}", name);
            SendMessage userNameSearchResponse = userHandler.handleUserSearchByName(chatId, name);
            executeMethod(userNameSearchResponse);
            return;
        } else if (userSearchNameMatcher.matches()) {
            String name = userSearchNameMatcher.group(1);
            log.info("Найдена команда поиска пользователя: /search_user {}", name);
            SendMessage userSearchResponse = userHandler.handleUserSearchByName(chatId, name);
            executeMethod(userSearchResponse);
            return;
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
                // Прямой вызов метода для отображения списка всех пополнений
                List<Transaction> transactions = transactionAdminService.getAllTransactions();
                StringBuilder message = new StringBuilder();
                message.append("📋 Пополнения (Все):\n\n");

                for (Transaction transaction : transactions) {
                    message.append("ID: ").append(transaction.getId()).append("\n");
                    message.append("Пользователь: ").append(transaction.getUser().getUsername()).append("\n");
                    message.append("Сумма: ").append(transaction.getAmount()).append(" ₽\n");
                    message.append("Статус: ").append(transactionAdminService.getStatusEmoji(transaction.getStatus())).append(" ").append(transaction.getStatus()).append("\n");
                    message.append("Дата: ").append(transactionAdminService.formatDateTime(transaction.getCreatedAt())).append("\n");
                    message.append("Код: ").append(transaction.getTransactionCode()).append("\n");
                    message.append("/deposit").append(transaction.getId()).append(" - подробности\n");
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
                sendMessage.setReplyMarkup(keyboardMarkup);

                yield sendMessage;
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
            } else if (data.startsWith("deposit:")) {
                handleDepositDetailsCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposit_confirm_")) {
                handleDepositConfirmCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposit_reject_")) {
                handleDepositRejectCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposit_details_")) {
                handleDepositDetailsCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("deposits:")) {
                handleDepositsFilterCallback(chatId, callbackId, data, messageId);
            } else if (data.startsWith("product:")) {
                handleProductCallback(chatId, data, messageId);
            } else if (data.startsWith("updateOrder:")) {
                handleUpdateOrderCallback(chatId, data, messageId);
            } else if (data.startsWith("viewUser:")) {
                // Обработка запроса на просмотр детальной информации о пользователе
                Long userId = Long.parseLong(data.substring("viewUser:".length()));
                SendMessage response = userHandler.handleUserDetails(chatId, userId);
                    executeMethod(response);
            } else if (data.startsWith("userOrders:")) {
                // Обработка запроса на просмотр заказов пользователя
                Long userId = Long.parseLong(data.substring("userOrders:".length()));
                SendMessage response = orderHandler.handleUserOrders(chatId, userId);
                executeMethod(response);
            } else if (data.startsWith("userNFTs:")) {
                // Обработка запроса на просмотр NFT пользователя
                Long userId = Long.parseLong(data.substring("userNFTs:".length()));
                SendMessage response = nftHandler.handleUserNFTs(chatId, userId);
                executeMethod(response);
            } else if (data.startsWith("user:deactivate:")) {
                // Обработка запроса на деактивацию пользователя
                Long userId = Long.parseLong(data.substring("user:deactivate:".length()));
                SendMessage response = userHandler.handleToggleUserStatus(chatId, userId, messageId);
                executeMethod(response);
            } else if (data.startsWith("user:activate:")) {
                // Обработка запроса на активацию пользователя
                Long userId = Long.parseLong(data.substring("user:activate:".length()));
                SendMessage response = userHandler.handleToggleUserStatus(chatId, userId, messageId);
                executeMethod(response);
            } else if (data.equals("listUsers")) {
                // Обработка запроса на получение списка пользователей
                SendMessage response = userHandler.handleListUsers(chatId);
                executeMethod(response);
            } else if (data.equals("searchUser")) {
                // Обработка запроса на поиск пользователя
                SendMessage response = userHandler.handleSearchUser(chatId);
                executeMethod(response);
            } else if (data.equals("searchUserByName")) {
                // Обработка запроса на поиск пользователя по имени
                SendMessage response = userHandler.handleSearchUserByName(chatId);
                executeMethod(response);
            } else if (data.equals("searchUserByEmail")) {
                // Обработка запроса на поиск пользователя по email
                SendMessage response = userHandler.handleSearchUserByEmail(chatId);
                executeMethod(response);
            } else if (data.equals("searchUserByPhone")) {
                // Обработка запроса на поиск пользователя по телефону
                SendMessage response = userHandler.handleSearchUserByPhone(chatId);
                executeMethod(response);
            } else if (data.equals("menu")) {
                // Обработка запроса на возврат в главное меню
                showMainMenu(chatId);
            }
            
            // Отправляем пустой ответ на callback, чтобы убрать "часики" на кнопке
            sendCallbackAnswer(callbackId, "", false);
        } catch (Exception e) {
            log.error("Ошибка при обработке callback-запроса: {}", e.getMessage(), e);
            sendCallbackAnswer(callbackId, "Ошибка: " + e.getMessage(), true);
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

    /**
     * Обрабатывает запрос на подтверждение пополнения
     */
    private void handleDepositConfirmCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на подтверждение пополнения: {}", data);
        try {
            Long transactionId = Long.parseLong(data.substring("deposit_confirm_".length()));
            
            // Получаем транзакцию через сервис
            Optional<Transaction> transactionOpt = transactionAdminService.getTransactionDetails(transactionId);
            if (transactionOpt.isEmpty()) {
                log.warn("Транзакция с ID {} не найдена в базе данных", transactionId);
                sendCallbackAnswer(callbackId, "❌ Транзакция не найдена", true);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            // Проверяем возможность подтверждения
            if (!transactionAdminService.canConfirmTransaction(transaction)) {
                log.warn("Попытка подтвердить транзакцию в статусе {}: {}", 
                        transaction.getStatus(), transaction.getTransactionCode());
                sendCallbackAnswer(callbackId, "❌ Невозможно подтвердить транзакцию в статусе " + transaction.getStatus(), true);
                return;
            }
            
            log.info("Подтверждение транзакции с кодом: {}", transaction.getTransactionCode());
            // Подтверждаем транзакцию через сервис
            Transaction confirmedTransaction = transactionAdminService.confirmDeposit(
                transaction.getTransactionCode(), 
                "Admin: " + chatId
            );
            
            log.info("Транзакция успешно подтверждена: ID={}, новый статус={}", 
                    confirmedTransaction.getId(), confirmedTransaction.getStatus());
            
            // Отправляем уведомление администратору
            SendMessage successMessage = createMessage(
                chatId, 
                "✅ Транзакция #" + transactionId + " успешно подтверждена. Баланс пользователя " + 
                escapeMarkdown(transaction.getUser().getUsername()) + " пополнен на " + transaction.getAmount() + " ₽."
            );
            executeMethod(successMessage);
            
                sendCallbackAnswer(callbackId, "Транзакция подтверждена", false);
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
    private void handleDepositRejectCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на отклонение пополнения: {}", data);
        try {
            Long transactionId = Long.parseLong(data.substring("deposit_reject_".length()));
            
            // Получаем транзакцию через сервис
            Optional<Transaction> transactionOpt = transactionAdminService.getTransactionDetails(transactionId);
            if (transactionOpt.isEmpty()) {
                log.warn("Транзакция с ID {} не найдена в базе данных", transactionId);
                sendCallbackAnswer(callbackId, "❌ Транзакция не найдена", true);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            // Проверяем возможность отклонения
            if (!transactionAdminService.canConfirmTransaction(transaction)) {
                log.warn("Попытка отклонить транзакцию в статусе {}: {}", 
                        transaction.getStatus(), transaction.getTransactionCode());
                sendCallbackAnswer(callbackId, "❌ Невозможно отклонить транзакцию в статусе " + transaction.getStatus(), true);
                return;
            }
            
            log.info("Отклонение транзакции с кодом: {}", transaction.getTransactionCode());
            // Отклоняем транзакцию через сервис
            Transaction rejectedTransaction = transactionAdminService.rejectDeposit(
                transaction.getTransactionCode(), 
                "Отклонено администратором", 
                "Admin: " + chatId
            );
            
            log.info("Транзакция успешно отклонена: ID={}, новый статус={}", 
                    rejectedTransaction.getId(), rejectedTransaction.getStatus());
            
            // Отправляем уведомление администратору
            SendMessage successMessage = createMessage(
                chatId, 
                "❌ Транзакция #" + transactionId + " отклонена. Пользователь " + 
                escapeMarkdown(transaction.getUser().getUsername()) + " был уведомлен."
            );
            executeMethod(successMessage);
            
                sendCallbackAnswer(callbackId, "Транзакция отклонена", false);
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
    private void handleDepositDetailsCallback(String chatId, String callbackId, String data, Integer messageId) {
        log.info(">> Обработка callback-запроса на просмотр деталей пополнения: {}", data);
        try {
            Long transactionId = Long.parseLong(data.substring("deposit_details_".length()));
            
            log.info("Получение деталей транзакции ID: {}", transactionId);
            // Получаем транзакцию через сервис
            Optional<Transaction> transactionOpt = transactionAdminService.getTransactionDetails(transactionId);
            if (transactionOpt.isEmpty()) {
                log.warn("Транзакция с ID {} не найдена в базе данных", transactionId);
                sendCallbackAnswer(callbackId, "❌ Транзакция не найдена", true);
                SendMessage errorMessage = createMessage(chatId, "❌ Транзакция не найдена.");
                executeMethod(errorMessage);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            // Теперь мы можем безопасно получить данные пользователя, так как он загружен жадно
            String username = escapeMarkdown(transaction.getUser().getUsername());
            String email = transaction.getUser().getEmail() != null ? escapeMarkdown(transaction.getUser().getEmail()) : "-";
            
            StringBuilder message = new StringBuilder();
            message.append("*📋 ДЕТАЛИ ПОПОЛНЕНИЯ #").append(transactionId).append("*\n\n");
            message.append("*Пользователь:* ").append(username).append("\n");
            message.append("*Email:* ").append(email).append("\n");
            message.append("*ID пользователя:* ").append(transaction.getUser().getId()).append("\n");
            message.append("*Сумма:* ").append(transaction.getAmount()).append(" ₽\n");
            message.append("*Статус:* ").append(transactionAdminService.getStatusEmoji(transaction.getStatus())).append(" ").append(transaction.getStatus()).append("\n");
            message.append("*Код транзакции:* `").append(transaction.getTransactionCode()).append("`\n");
            message.append("*Тип:* ").append(transaction.getType()).append("\n");
            message.append("*Создана:* ").append(transactionAdminService.formatDateTime(transaction.getCreatedAt())).append("\n");
            
            if (transaction.getUpdatedAt() != null) {
                message.append("*Обновлена:* ").append(transactionAdminService.formatDateTime(transaction.getUpdatedAt())).append("\n");
            }
            
            if (transaction.getAdminComment() != null && !transaction.getAdminComment().isEmpty()) {
                message.append("*Комментарий админа:* ").append(escapeMarkdown(transaction.getAdminComment())).append("\n");
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
            
            executeMethod(sendMessage);
            sendCallbackAnswer(callbackId, "Детали транзакции", false);
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
     * Метод для непосредственного вызова из handleCallbackQuery
     */
    private void handleDepositsFilterCallback(String chatId, String callbackId, String data, Integer messageId) {
        try {
            String filter = data.substring("deposits:".length());
            List<Transaction> transactions;
            String title;
            
            // Определяем, какие транзакции загружать в зависимости от фильтра
            switch (filter) {
                case "all":
                    title = "Все пополнения";
                    transactions = transactionAdminService.getAllTransactions();
                    break;
                case "pending":
                    title = "Ожидающие пополнения";
                    transactions = transactionAdminService.getTransactionsByStatus(TransactionStatus.PENDING);
                    break;
                case "completed":
                    title = "Завершенные пополнения";
                    transactions = transactionAdminService.getTransactionsByStatus(TransactionStatus.COMPLETED);
                    break;
                case "rejected":
                    title = "Отклоненные пополнения";
                    transactions = transactionAdminService.getTransactionsByStatus(TransactionStatus.REJECTED);
                    break;
                case "today":
                    title = "Пополнения за сегодня";
                    transactions = transactionAdminService.getTransactionsToday();
                    break;
                case "week":
                    title = "Пополнения за неделю";
                    transactions = transactionAdminService.getTransactionsThisWeek();
                    break;
                case "month":
                    title = "Пополнения за месяц";
                    transactions = transactionAdminService.getTransactionsThisMonth();
                    break;
                default:
                    sendMessage(chatId, "❌ Неизвестный фильтр: " + filter);
                    return;
            }
            
            // Если нет транзакций, показываем сообщение об этом
            if (transactions.isEmpty()) {
                SendMessage emptyMessage = createMessage(chatId, "📋 Пополнения (" + title + ")\n\nНет пополнений для отображения.");
                executeMethod(emptyMessage);
                return;
    }
    
            // Формируем сообщение со списком транзакций
            StringBuilder message = new StringBuilder();
            message.append("📋 Пополнения (").append(title).append("):\n\n");

            for (Transaction transaction : transactions) {
                message.append("ID: ").append(transaction.getId()).append("\n");
                message.append("Пользователь: ").append(transaction.getUser().getUsername()).append("\n");
                message.append("Сумма: ").append(transaction.getAmount()).append(" ₽\n");
                message.append("Статус: ").append(transactionAdminService.getStatusEmoji(transaction.getStatus())).append(" ").append(transaction.getStatus()).append("\n");
                message.append("Дата: ").append(transactionAdminService.formatDateTime(transaction.getCreatedAt())).append("\n");
                message.append("Код: ").append(transaction.getTransactionCode()).append("\n");
                message.append("/deposit").append(transaction.getId()).append(" - подробности\n");
                message.append("-------------------\n");
            }

            // Создаем клавиатуру с фильтрами
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

            // Создаем и отправляем сообщение
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message.toString());
            sendMessage.setReplyMarkup(keyboardMarkup);

            executeMethod(sendMessage);
            
            // Отправляем ответ на callback, если он был предоставлен
            if (callbackId != null) {
                sendCallbackAnswer(callbackId, "Показаны " + title.toLowerCase(), false);
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке фильтра пополнений: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка: " + e.getMessage());
            
            // Отправляем ответ об ошибке на callback, если он был предоставлен
            if (callbackId != null) {
                sendCallbackAnswer(callbackId, "Ошибка: " + e.getMessage(), true);
            }
        }
    }
    
    /**
     * Обрабатывает запрос на обновление статуса заказа
     */
    private void handleUpdateOrderCallback(String chatId, String data, Integer messageId) {
        try {
            String[] parts = data.substring("updateOrder:".length()).split(":");
            if (parts.length != 2) {
                log.error("Неверный формат callback-данных для обновления статуса заказа: {}", data);
                return;
            }
            
            Long orderId = Long.parseLong(parts[0]);
            OrderStatus newStatus = OrderStatus.valueOf(parts[1]);
            
            log.info("Обработка запроса на обновление статуса заказа #{} на {}", orderId, newStatus);
            BotApiMethod<?> response = orderHandler.handleUpdateOrderStatus(chatId, orderId, newStatus, messageId);
            executeMethod(response);
        } catch (Exception e) {
            log.error("Ошибка при обработке обновления статуса заказа: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при обновлении статуса заказа: " + e.getMessage());
        }
    }

    /**
     * Обрабатывает запрос для товаров
     */
    private void handleProductCallback(String chatId, String data, Integer messageId) {
        try {
            String action = data.substring("product:".length());
            log.info("Обработка callback для товаров: {}", action);
            
            if (action.equals("all")) {
                executeMethod(productHandler.handleAllProducts(chatId));
            } else if (action.equals("search")) {
                executeMethod(productHandler.handleProductSearchRequest(chatId));
            } else if (action.equals("create")) {
                executeMethod(productHandler.handleCreateProductRequest(chatId));
            } else if (action.startsWith("edit:")) {
                Long productId = Long.parseLong(action.substring("edit:".length()));
                // Нет метода для редактирования названия товара
                executeMethod(productHandler.handleProductDetails(chatId, productId));
            } else if (action.startsWith("price:")) {
                Long productId = Long.parseLong(action.substring("price:".length()));
                executeMethod(productHandler.handleUpdatePriceRequest(chatId, productId));
            } else if (action.startsWith("stock:")) {
                Long productId = Long.parseLong(action.substring("stock:".length()));
                executeMethod(productHandler.handleUpdateStockRequest(chatId, productId));
            } else if (action.startsWith("delete:")) {
                Long productId = Long.parseLong(action.substring("delete:".length()));
                executeMethod(productHandler.handleDeleteProductRequest(chatId, productId));
            } else if (action.startsWith("details:") || action.startsWith("view:")) {
                Long productId;
                if (action.startsWith("details:")) {
                    productId = Long.parseLong(action.substring("details:".length()));
                } else {
                    productId = Long.parseLong(action.substring("view:".length()));
                }
                executeMethod(productHandler.handleProductDetails(chatId, productId));
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке callback для товаров: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Ошибка при обработке запроса для товаров: " + e.getMessage());
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
     * Создаёт объект сообщения с клавиатурой
     */
    private SendMessage createMessage(String chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard keyboard) {
        SendMessage message = createMessage(chatId, text);
        message.setReplyMarkup(keyboard);
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
     * Выполняет метод API бота с обработкой ошибок
     */
    private void executeMethod(BotApiMethod<?> method) {
        if (method == null) {
            log.warn("Попытка выполнить null метод");
            return;
        }
        
        try {
            String methodType = method.getClass().getSimpleName();
            log.debug("Выполнение метода API: {}", methodType);
            
            Object result = execute(method);
            
            log.debug("Метод {} успешно выполнен", methodType);
        } catch (TelegramApiException e) {
            log.error("Ошибка выполнения метода API бота: {} - {}", 
                    method.getClass().getSimpleName(), e.getMessage(), e);
            
            // Если это метод отправки сообщения, попробуем отправить сообщение об ошибке
            if (method instanceof SendMessage) {
                SendMessage sendMessage = (SendMessage) method;
                String chatId = sendMessage.getChatId();
                
                try {
                    SendMessage errorMessage = new SendMessage();
                    errorMessage.setChatId(chatId);
                    errorMessage.setText("❌ Произошла ошибка при выполнении запроса: " + e.getMessage());
                    execute(errorMessage);
                } catch (TelegramApiException ex) {
                    log.error("Не удалось отправить сообщение об ошибке: {}", ex.getMessage());
        }
            }
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при выполнении метода API: {}", e.getMessage(), e);
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
     * Обрабатывает поиск пользователя
     */
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
     * Создаёт кнопку с текстом и callback-данными
     */
    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\=|{}.])", "\\\\$1");
    }

    private BotApiMethod<?> handleDepositCommand(String chatId, Matcher depositMatcher) {
        try {
            Long depositId = Long.parseLong(depositMatcher.group(1));
            log.info("Admin {} requested deposit details: {}", chatId, depositId);
            
            // Получаем транзакцию
            Optional<Transaction> transactionOpt = transactionAdminService.getTransactionDetails(depositId);
            if (transactionOpt.isEmpty()) {
                log.warn("Транзакция #{} не найдена в базе данных", depositId);
                return createMessage(chatId, "❌ Транзакция #" + depositId + " не найдена.");
            }

            Transaction transaction = transactionOpt.get();
            
            // Формируем текст с информацией о транзакции
            StringBuilder messageText = new StringBuilder();
            messageText.append("📋 ДЕТАЛИ ПОПОЛНЕНИЯ #").append(depositId).append("\n\n");
            messageText.append("Пользователь: ").append(transaction.getUser().getUsername()).append("\n");
            messageText.append("Email: ").append(transaction.getUser().getEmail() != null ? transaction.getUser().getEmail() : "-").append("\n");
            messageText.append("ID пользователя: ").append(transaction.getUser().getId()).append("\n");
            messageText.append("Сумма: ").append(transaction.getAmount()).append(" ₽\n");
            messageText.append("Статус: ").append(transactionAdminService.getStatusEmoji(transaction.getStatus())).append(" ").append(transaction.getStatus()).append("\n");
            messageText.append("Код транзакции: ").append(transaction.getTransactionCode()).append("\n");
            messageText.append("Тип: ").append(transaction.getType()).append("\n");
            messageText.append("Создана: ").append(transactionAdminService.formatDateTime(transaction.getCreatedAt())).append("\n");
            
            if (transaction.getUpdatedAt() != null) {
                messageText.append("Обновлена: ").append(transactionAdminService.formatDateTime(transaction.getUpdatedAt())).append("\n");
            }
            
            if (transaction.getAdminComment() != null && !transaction.getAdminComment().isEmpty()) {
                messageText.append("Комментарий админа: ").append(transaction.getAdminComment()).append("\n");
            }

            // Создаем клавиатуру с действиями в зависимости от статуса транзакции
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            // Если статус PENDING, добавляем кнопки подтверждения/отклонения
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                List<InlineKeyboardButton> actionRow = new ArrayList<>();
                
                InlineKeyboardButton confirmButton = new InlineKeyboardButton();
                confirmButton.setText("✅ Подтвердить");
                confirmButton.setCallbackData("deposit_confirm_" + depositId);
                actionRow.add(confirmButton);
                
                InlineKeyboardButton rejectButton = new InlineKeyboardButton();
                rejectButton.setText("❌ Отклонить");
                rejectButton.setCallbackData("deposit_reject_" + depositId);
                actionRow.add(rejectButton);
                
                keyboard.add(actionRow);
            }
            
            // Кнопка возврата
            List<InlineKeyboardButton> backRow = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀️ Назад к списку");
            backButton.setCallbackData("deposits:all");
            backRow.add(backButton);
            keyboard.add(backRow);
            
            keyboardMarkup.setKeyboard(keyboard);
            
            // Создаем сообщение
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(messageText.toString());
            message.setReplyMarkup(keyboardMarkup);
            
            return message;
        } catch (Exception e) {
            log.error("Ошибка при обработке команды просмотра депозита: {}", e.getMessage(), e);
            return createMessage(chatId, "❌ Ошибка при получении деталей депозита: " + e.getMessage());
        }
    }

    /**
     * Обработка меню (callback-запросы начинающиеся с menu:)
     */
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

    /**
     * Обработка промокодов (callback-запросы начинающиеся с promo:)
     */
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

    /**
     * Показывает главное меню
     */
    private void showMainMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 *Главное меню*\n\nВыберите раздел:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createMainMenu());
        executeMethod(message);
    }

    /**
     * Показывает меню заказов
     */
    private void showOrdersMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📦 *Управление заказами*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createOrderFiltersKeyboard());
        executeMethod(message);
    }

    /**
     * Показывает меню пользователей
     */
    private void showUsersMenu(String chatId) {
        log.info("Отображение меню пользователей для администратора {}", chatId);
        try {
            // Вызываем метод из UserHandler, который возвращает список пользователей с клавиатурой
            SendMessage response = userHandler.handleListUsers(chatId);
            execute(response);
        } catch (Exception e) {
            log.error("Ошибка при отображении меню пользователей: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ Произошла ошибка при отображении меню пользователей: " + e.getMessage());
        }
    }

    /**
     * Показывает меню промокодов
     */
    private void showPromoCodesMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎟 *Управление промокодами*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createPromoCodesKeyboard());
        executeMethod(message);
    }

    /**
     * Показывает меню товаров
     */
    private void showProductsMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🛍 *Управление товарами*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createProductsKeyboard());
        executeMethod(message);
    }

    /**
     * Показывает меню настроек
     */
    private void showSettingsMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚙️ *Настройки*\n\nВыберите действие:");
        message.setParseMode("Markdown");
        message.setReplyMarkup(AdminKeyboards.createSettingsKeyboard());
        executeMethod(message);
    }
}
