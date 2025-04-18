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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AdminTelegramBot extends TelegramLongPollingBot {

    private final OrderRepository orderRepository;
    private final OrderHandler orderHandler;
    private final UserHandler userHandler;
    private final AdminBotService adminBotService;
    private final PromoCodeHandler promoCodeHandler;
    private final ProductHandler productHandler;

    @Value("${admin.bot.username}")
    private String botUsername;

    @Value("${admin.bot.token}")
    private String botToken;

    // Список разрешенных Telegram ID, заданный в конфигурационном файле (через запятую)
    @Value("${admin.bot.adminIds}")
    private String adminIds;

    private Set<String> allowedAdminIds;
    
    // Регулярное выражение для обработки команд вида /order_123
    private static final Pattern ORDER_COMMAND_PATTERN = Pattern.compile("/order_(\\d+)");
    // Регулярное выражение для обработки команд вида /promo_123
    private static final Pattern PROMO_COMMAND_PATTERN = Pattern.compile("/promo_(\\d+)");
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
    private static final Pattern ORDER_SEARCH_PATTERN = Pattern.compile("/order_search\\s+(.+)$");
    // Регулярное выражение для обработки команд вида /promo_create CODE 15% 100 Description
    private static final Pattern PROMO_CREATE_PATTERN = Pattern.compile("^/promo_create\\s+(\\S+)\\s+(\\d+)%\\s+(\\d+)(?:\\s+(.+))?$");
    // Регулярное выражение для обработки команд вида /promo_edit_123 CODE 15% 100 Description
    private static final Pattern PROMO_EDIT_PATTERN = Pattern.compile("^/promo_edit_(\\d+)\\s+([A-Z0-9]+)\\s+(\\d+)%\\s+(\\d+)\\s+(.+)$");
    // Регулярное выражение для обработки команд вида /user_search query
    private static final Pattern USER_SEARCH_PATTERN = Pattern.compile("^/user_search\\s+(.+)$");
    // Регулярное выражение для обработки команд вида /usersearch query (альтернативная форма)
    private static final Pattern USER_SEARCH_ALT_PATTERN = Pattern.compile("^/usersearch\\s+(.+)$");
    // Регулярное выражение для обработки команд вида /user_123
    private static final Pattern USER_COMMAND_PATTERN = Pattern.compile("/user_(\\d+)");

    // Конструктор с инициализацией всех необходимых полей
    public AdminTelegramBot(
            @Value("${admin.bot.token}") String botToken,
            OrderRepository orderRepository,
            OrderHandler orderHandler,
            UserHandler userHandler,
            AdminBotService adminBotService,
            PromoCodeHandler promoCodeHandler,
            ProductHandler productHandler) {
        super(botToken);
        this.orderRepository = orderRepository;
        this.orderHandler = orderHandler;
        this.userHandler = userHandler;
        this.adminBotService = adminBotService;
        this.promoCodeHandler = promoCodeHandler;
        this.productHandler = productHandler;
    }

    @PostConstruct
    public void init() {
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
        BotApiMethod<?> response = null;
        
        // Проверяем, соответствует ли текст команде для просмотра заказа
        Matcher orderMatcher = ORDER_COMMAND_PATTERN.matcher(text);
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
        
        if (orderMatcher.matches()) {
            Long orderId = Long.parseLong(orderMatcher.group(1));
            response = orderHandler.handleOrderDetails(chatId, orderId);
        } else if (promoMatcher.matches()) {
            Long promoId = Long.parseLong(promoMatcher.group(1));
            response = promoCodeHandler.handlePromoCodeDetails(chatId, promoId);
        } else if (productMatcher.matches()) {
            Long productId = Long.parseLong(productMatcher.group(1));
            response = productHandler.handleProductDetails(chatId, productId);
        } else if (productSearchMatcher.matches()) {
            String query = productSearchMatcher.group(1);
            response = productHandler.handleProductSearch(chatId, query);
        } else if (productPriceMatcher.matches()) {
            Long productId = Long.parseLong(productPriceMatcher.group(1));
            String price = productPriceMatcher.group(2);
            response = productHandler.handleUpdatePrice(chatId, productId, price);
        } else if (productStockMatcher.matches()) {
            Long productId = Long.parseLong(productStockMatcher.group(1));
            String stock = productStockMatcher.group(2);
            response = productHandler.handleUpdateStock(chatId, productId, stock);
        } else if (productCreateMatcher.matches()) {
            String productData = productCreateMatcher.group(1);
            response = productHandler.handleCreateProduct(chatId, productData);
        } else if (orderSearchMatcher.matches()) {
            Matcher matcher = ORDER_SEARCH_PATTERN.matcher(text);
            if (matcher.find()) {
                response = orderHandler.handleOrderSearch(chatId, matcher.group(1));
            }
        } else if (promoCreateMatcher.matches()) {
            String promoData = promoCreateMatcher.group(0).substring("/promo_create ".length());
            response = promoCodeHandler.handleCreatePromoCode(chatId, promoData);
        } else if (promoEditMatcher.matches()) {
            Matcher matcher = PROMO_EDIT_PATTERN.matcher(text);
            if (matcher.find()) {
                Long promoId = Long.parseLong(matcher.group(1));
                String code = matcher.group(2);
                int discount = Integer.parseInt(matcher.group(3));
                int maxUses = Integer.parseInt(matcher.group(4));
                String description = matcher.group(5);
                response = promoCodeHandler.handleUpdatePromoCode(chatId, promoId, code, discount, maxUses, description);
            }
        } else if (userSearchMatcher.matches()) {
            Matcher matcher = USER_SEARCH_PATTERN.matcher(text);
            if (matcher.find()) {
                response = userHandler.handleUserSearch(chatId, matcher.group(1));
            }
        } else if (userSearchAltMatcher.matches()) {
            Matcher matcher = USER_SEARCH_ALT_PATTERN.matcher(text);
            if (matcher.find()) {
                log.info("Получен запрос на поиск пользователя (альт): {}", matcher.group(1));
                response = userHandler.handleUserSearch(chatId, matcher.group(1));
            }
        } else if (userMatcher.matches()) {
            Long userId = Long.parseLong(userMatcher.group(1));
            response = userHandler.handleUserDetails(chatId, userId);
        } else {
            // Обрабатываем стандартные команды
            response = switch (text) {
                case "/start", "/help" -> createWelcomeMessage(chatId);
                case "/orders", "📋 Все заказы" -> orderHandler.handleAllOrders(chatId);
                case "/stats", "📊 Статистика" -> orderHandler.handleOrderStatistics(chatId);
                case "/users", "👤 Пользователи" -> userHandler.handleUserList(chatId);
                case "/menu" -> createMainMenuMessage(chatId);
                case "🔍 Поиск заказа" -> orderHandler.handleOrderSearchRequest(chatId);
                case "🎨 NFT" -> createNFTMenuMessage(chatId);
                case "⚙️ Настройки" -> createSettingsMessage(chatId);
                case "/promo", "🔖 Промокоды" -> promoCodeHandler.handleAllPromoCodes(chatId);
                case "/products", "👕 Товары" -> productHandler.handleAllProducts(chatId);
                default -> createUnknownCommandMessage(chatId);
            };
        }
        
        executeMethod(response);
    }

    /**
     * Обработка callback-запросов (нажатие inline-кнопок)
     */
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String chatId = callbackQuery.getMessage().getChatId().toString();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        
        // Проверяем, что отправитель - администратор
        if (!isAdmin(chatId)) {
            sendCallbackAnswer(callbackQuery.getId(), "Доступ запрещен.", true);
            return;
        }
        
        String callbackData = callbackQuery.getData();
        log.debug("Received callback query: {}", callbackData);
        
        BotApiMethod<?> response = null;
        
        try {
            if (callbackData.startsWith("filter:")) {
                response = handleOrdersFilterCallback(chatId, callbackData.substring(7), messageId);
            } else if (callbackData.startsWith("updateOrder:")) {
                String[] parts = callbackData.substring(12).split(":");
                if (parts.length >= 2) {
                    Long orderId = Long.parseLong(parts[0]);
                    OrderStatus newStatus = OrderStatus.valueOf(parts[1]);
                    response = orderHandler.handleUpdateOrderStatus(chatId, orderId, newStatus, messageId);
                }
            } else if (callbackData.startsWith("viewUser:")) {
                Long userId = Long.parseLong(callbackData.substring(9));
                response = userHandler.handleUserDetails(chatId, userId);
            } else if (callbackData.startsWith("userOrders:")) {
                Long userId = Long.parseLong(callbackData.substring(11));
                response = orderHandler.handleUserOrders(chatId, userId);
            } else if (callbackData.startsWith("userNFTs:")) {
                Long userId = Long.parseLong(callbackData.substring(9));
                response = handleUserNFTs(chatId, userId);
            } else if (callbackData.startsWith("toggleUserStatus:")) {
                Long userId = Long.parseLong(callbackData.substring(17));
                response = userHandler.handleToggleUserStatus(chatId, userId, messageId);
            } else if (callbackData.startsWith("stats:")) {
                response = handleStatsCallback(chatId, callbackData.substring(6), messageId);
            } else if (callbackData.startsWith("nft:")) {
                response = handleNftCallback(chatId, callbackData.substring(4), messageId);
            } else if (callbackData.startsWith("promo:")) {
                response = handlePromoCallback(chatId, callbackData.substring(6), messageId);
            } else if (callbackData.startsWith("product:")) {
                response = handleProductCallback(chatId, callbackData.substring(8), messageId);
            } else if (callbackData.startsWith("menu:")) {
                response = handleMenuCallback(chatId, callbackData.substring(5));
            } else if (callbackData.startsWith("user:")) {
                String[] parts = callbackData.substring(5).split(":");
                if (parts.length >= 2) {
                    Long userId = Long.parseLong(parts[1]);
                    if (parts[0].equals("activate") || parts[0].equals("deactivate")) {
                        response = userHandler.handleToggleUserStatus(chatId, userId, messageId);
                    }
                }
            } else if (callbackData.equals("searchUser")) {
                response = userHandler.handleSearchUserForm(chatId);
            } else if (callbackData.equals("listUsers")) {
                response = userHandler.handleUserList(chatId);
            }
            
            if (response != null) {
                execute(response);
            }
            
            sendCallbackAnswer(callbackQuery.getId(), "✓", false);
            
        } catch (Exception e) {
            log.error("Error handling callback query: ", e);
            try {
                sendCallbackAnswer(callbackQuery.getId(), "❌ Ошибка: " + e.getMessage(), true);
            } catch (Exception ex) {
                log.error("Error sending callback answer: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Обработка callback фильтрации заказов
     */
    private BotApiMethod<?> handleOrdersFilterCallback(String chatId, String filter, Integer messageId) {
        return switch (filter) {
            case "all" -> orderHandler.handleAllOrders(chatId);
            case "NEW" -> orderHandler.handleOrdersByStatus(chatId, OrderStatus.NEW);
            case "PROCESSING" -> orderHandler.handleOrdersByStatus(chatId, OrderStatus.PROCESSING);
            case "DISPATCHED" -> orderHandler.handleOrdersByStatus(chatId, OrderStatus.DISPATCHED);
            case "COMPLETED" -> orderHandler.handleOrdersByStatus(chatId, OrderStatus.COMPLETED);
            case "CANCELLED" -> orderHandler.handleOrdersByStatus(chatId, OrderStatus.CANCELLED);
            case "today" -> orderHandler.handleTodayOrders(chatId);
            case "week" -> orderHandler.handleWeekOrders(chatId);
            case "month" -> orderHandler.handleMonthOrders(chatId);
            case "search" -> orderHandler.handleOrderSearchRequest(chatId);
            default -> createMainMenuMessage(chatId);
        };
    }

    /**
     * Обработка callback статистики
     */
    private BotApiMethod<?> handleStatsCallback(String chatId, String statType, Integer messageId) {
        return switch (statType) {
            case "general" -> orderHandler.handleOrderStatistics(chatId);
            case "daily" -> orderHandler.handleDailyStatistics(chatId);
            case "topUsers" -> orderHandler.handleTopUsers(chatId);
            case "topProducts" -> createTopProductsMessage(chatId);
            default -> orderHandler.handleOrderStatistics(chatId);
        };
    }

    /**
     * Обработка callback меню
     */
    private BotApiMethod<?> handleMenuCallback(String chatId, String menuItem) {
        return switch (menuItem) {
            case "main" -> createMainMenuMessage(chatId);
            case "users" -> userHandler.handleUserList(chatId);
            default -> createMainMenuMessage(chatId);
        };
    }

    /**
     * Обработка callback NFT
     */
    private BotApiMethod<?> handleNftCallback(String chatId, String nftCommand, Integer messageId) {
        return switch (nftCommand) {
            case "all" -> createAllNFTsMessage(chatId);
            case "unrevealed" -> createUnrevealedNFTsMessage(chatId);
            case "searchByUser" -> createNFTSearchMessage(chatId);
            default -> createNFTMenuMessage(chatId);
        };
    }

    /**
     * Обрабатывает callback-запросы для промокодов
     */
    private BotApiMethod<?> handlePromoCallback(String chatId, String command, Integer messageId) {
        if (command.equals("all")) {
            return promoCodeHandler.handleAllPromoCodes(chatId);
        } else if (command.equals("active")) {
            return promoCodeHandler.handleActivePromoCodes(chatId);
        } else if (command.equals("create")) {
            return promoCodeHandler.handleCreatePromoCodeRequest(chatId);
        } else if (command.startsWith("activate:")) {
            Long promoId = Long.parseLong(command.substring(9));
            return promoCodeHandler.handleActivatePromoCode(chatId, messageId, promoId);
        } else if (command.startsWith("deactivate:")) {
            Long promoId = Long.parseLong(command.substring(11));
            return promoCodeHandler.handleDeactivatePromoCode(chatId, messageId, promoId);
        } else if (command.startsWith("delete:")) {
            Long promoId = Long.parseLong(command.substring(7));
            return promoCodeHandler.handleDeletePromoCode(chatId, messageId, promoId);
        } else if (command.startsWith("edit:")) {
            Long promoId = Long.parseLong(command.substring(5));
            return promoCodeHandler.handleEditPromoCodeRequest(chatId, promoId);
        } else if (command.startsWith("details:")) {
            Long promoId = Long.parseLong(command.substring(8));
            return promoCodeHandler.handlePromoCodeDetails(chatId, promoId);
        } else if (command.equals("cancel")) {
            // Отмена действия, возвращаем к списку промокодов
            return promoCodeHandler.handleAllPromoCodes(chatId);
        }
        
        return createUnknownCommandMessage(chatId);
    }
    
    /**
     * Обрабатывает callback-запросы для товаров
     */
    private BotApiMethod<?> handleProductCallback(String chatId, String command, Integer messageId) {
        if (command.equals("all")) {
            return productHandler.handleAllProducts(chatId);
        } else if (command.equals("search")) {
            return productHandler.handleProductSearchRequest(chatId);
        } else if (command.equals("create")) {
            return productHandler.handleCreateProductRequest(chatId);
        } else if (command.startsWith("details:")) {
            Long productId = Long.parseLong(command.substring(8));
            return productHandler.handleProductDetails(chatId, productId);
        } else if (command.startsWith("price:")) {
            Long productId = Long.parseLong(command.substring(6));
            return productHandler.handleUpdatePriceRequest(chatId, productId);
        } else if (command.startsWith("stock:")) {
            Long productId = Long.parseLong(command.substring(6));
            return productHandler.handleUpdateStockRequest(chatId, productId);
        } else if (command.startsWith("delete:")) {
            Long productId = Long.parseLong(command.substring(7));
            return productHandler.handleDeleteProductRequest(chatId, productId);
        } else if (command.startsWith("confirmDelete:")) {
            Long productId = Long.parseLong(command.substring(14));
            return productHandler.handleDeleteProduct(chatId, productId);
        }
        
        return createUnknownCommandMessage(chatId);
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
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(text);
        answer.setShowAlert(isError);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки ответа на callback: {}", e.getMessage());
        }
    }

    /**
     * Выполняет метод API бота
     */
    private void executeMethod(BotApiMethod<?> method) {
        if (method == null) {
            return;
        }
        
        try {
            execute(method);
        } catch (TelegramApiException e) {
            log.error("Ошибка выполнения метода API бота: {}", e.getMessage());
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
}
