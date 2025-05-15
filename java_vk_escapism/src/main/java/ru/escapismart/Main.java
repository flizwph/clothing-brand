package ru.escapismart;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.*;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import com.vk.api.sdk.objects.photos.responses.MessageUploadResponse;
import com.vk.api.sdk.queries.messages.MessagesGetLongPollHistoryQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.config.AppConfig;
import ru.escapismart.dao.CryptoAlertDao;
import ru.escapismart.dao.OrderDao;
import ru.escapismart.dao.PaymentDao;
import ru.escapismart.dao.UserDao;
import ru.escapismart.keyboard.VkKeyboardFactory;
import ru.escapismart.model.CryptoAlert;
import ru.escapismart.model.Order;
import ru.escapismart.model.Payment;
import ru.escapismart.model.User;
import ru.escapismart.service.CryptoAlertService;
import ru.escapismart.service.PaymentService;
import ru.escapismart.service.TokenChartService;
import ru.escapismart.util.HibernateUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    // Конфигурация приложения
    private static AppConfig config;
    
    // Клиент API ВКонтакте
    private static VkApiClient vk;
    private static GroupActor groupActor;
    
    // DAO объекты для работы с базой данных
    private static UserDao userDao;
    private static OrderDao orderDao;
    private static PaymentDao paymentDao;
    private static CryptoAlertDao cryptoAlertDao;
    
    // Сервисы для работы с графиками токенов и платежами
    private static TokenChartService tokenChartService;
    private static PaymentService paymentService;
    private static CryptoAlertService cryptoAlertService;
    
    // Временное хранилище состояний пользователей
    private static final Map<Long, Map<String, Object>> userStates = new HashMap<>();
    
    // Random для генерации random_id
    private static final Random random = new Random();
    
    // Регулярное выражение для проверки ссылок на товары
    private static final Pattern MARKET_PRODUCT_PATTERN = Pattern.compile("https?://vk\\.com/market/product/\\d+-\\d+-\\d+");
    
    public static void main(String[] args) {
        try {
            // Загрузка конфигурации
            config = AppConfig.getInstance();
            logger.info("Конфигурация загружена успешно");

            // Инициализация DAO
            userDao = new UserDao();
            orderDao = new OrderDao();
            paymentDao = new PaymentDao();
            cryptoAlertDao = new CryptoAlertDao();
            
            // Инициализация сервисов
            paymentService = new PaymentService();
            tokenChartService = TokenChartService.getInstance();
            cryptoAlertService = CryptoAlertService.getInstance();
            
            // Инициализация VK API клиента
            TransportClient transportClient = new HttpTransportClient();
            vk = new VkApiClient(transportClient);
            
            // Получение идентификатора группы и токена из конфигурации
            Integer groupId = config.getGroupId();
            String accessToken = config.getVkToken();
            
            // Создание актора группы
            groupActor = new GroupActor(groupId, accessToken);
            logger.info("Подключение к VK API выполнено успешно");
            
            // Запуск сервиса уведомлений о курсах криптовалют
            if (config.isPriceAlertsEnabled()) {
                cryptoAlertService.setVkClient(vk, groupActor);
                cryptoAlertService.startMonitoring();
                logger.info("Сервис уведомлений о курсах криптовалют запущен");
            }
            
            // Получение LongPoll сервера
            Integer ts = vk.messages().getLongPollServer(groupActor).execute().getTs();
            String server = vk.messages().getLongPollServer(groupActor).execute().getServer();
            String key = vk.messages().getLongPollServer(groupActor).execute().getKey();
            
            // Основной цикл обработки сообщений
            while (true) {
                try {
                    // Получение обновлений от LongPoll сервера
                    MessagesGetLongPollHistoryQuery historyQuery = vk.messages().getLongPollHistory(groupActor)
                        .ts(ts);
                    List<Message> messages = historyQuery.execute().getMessages().getItems();
                    
                    // Обработка каждого сообщения
                    for (Message message : messages) {
                        // Обработка сообщения пользователя
                        processUserMessage(vk, groupActor, message, userDao, orderDao, paymentService, tokenChartService);
                    }
                    
                    // Обновление TS для следующего запроса
                    ts = vk.messages().getLongPollServer(groupActor).execute().getTs();
                    
                } catch (ApiException | ClientException e) {
                    logger.error("Ошибка при получении событий: {}", e.getMessage());
                    // Короткая пауза перед следующей попыткой
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            logger.error("Критическая ошибка при запуске бота: {}", e.getMessage(), e);
        } finally {
            // Остановка сервиса уведомлений
            if (cryptoAlertService != null) {
                cryptoAlertService.stopMonitoring();
            }
            
            // Закрытие соединения с базой данных
            HibernateUtil.shutdown();
        }
    }
    
    /**
     * Обработка сообщения пользователя
     */
    private static void processUserMessage(VkApiClient vk, GroupActor groupActor, Message message, 
                                          UserDao userDao, OrderDao orderDao, 
                                          PaymentService paymentService, TokenChartService tokenChartService) {
        try {
            // Получение ID пользователя
            Integer fromId = message.getFromId();
            Long userId = fromId.longValue();
            logger.info("Получено сообщение от пользователя: {}", userId);
            
            // Проверяем, является ли пользователь админом
            boolean isAdmin = userId.intValue() == AppConfig.getInstance().getAdminUserId();
            
            // Получение или создание пользователя
            User user = userDao.getUser(userId);
            if (user == null) {
                user = new User(userId, null);
                userDao.save(user);
                logger.info("Создан новый пользователь: {}", userId);
            }
            
            // Обновление времени последнего взаимодействия
            user.setLastInteractionTime(new Date());
            userDao.save(user);
            
            // Получение текста сообщения
            String messageText = message.getText().trim();
            
            // Проверка на команды выхода и сброса состояния
            if (messageText.equals("Начать") || messageText.equals("/start")) {
                user.setLastState(null);
                userDao.save(user);
                sendWelcomeMessage(vk, groupActor, userId.intValue());
                return;
            }
            
            // Проверка на выход из чата с админом
            String userState = user.getLastState();
            if (messageText.equals("Выйти из чата с админом") && userState != null && userState.equals("CONTACTING_ADMIN")) {
                // Возврат в главное меню
                user.setLastState(null);
                userDao.save(user);
                sendMessage(vk, groupActor, userId.intValue(), "Вы вышли из режима связи с администратором.");
                sendDefaultKeyboard(vk, groupActor, userId.intValue());
                return;
            }
            
            // Обработка сообщений от администратора
            if (isAdmin && messageText.startsWith("ответ:")) {
                // Формат сообщения: "ответ:USER_ID:ТЕКСТ_СООБЩЕНИЯ"
                try {
                    String[] parts = messageText.split(":", 3);
                    if (parts.length == 3) {
                        Long targetUserId = Long.parseLong(parts[1].trim());
                        String responseText = parts[2].trim();
                        
                        // Отправляем ответ пользователю
                        sendMessage(vk, groupActor, targetUserId.intValue(), "Ответ администратора:\n\n" + responseText);
                        sendMessage(vk, groupActor, userId.intValue(), "Ответ успешно отправлен пользователю " + targetUserId);
                        return;
                    }
                } catch (NumberFormatException e) {
                    sendMessage(vk, groupActor, userId.intValue(), "Неверный формат команды. Используйте: ответ:ID_ПОЛЬЗОВАТЕЛЯ:ТЕКСТ_СООБЩЕНИЯ");
                    return;
                }
            }
            
            // Обработка команды профиля пользователя
            if (messageText.equals("👤 Мой профиль") || messageText.equals("Мой профиль")) {
                // Отображаем профиль пользователя
                String profileInfo = user.getProfileInfo();
                sendMessageWithKeyboard(vk, groupActor, userId.intValue(), profileInfo, VkKeyboardFactory.getProfileKeyboard());
                return;
            }
            
            // Проверка на возврат в меню
            if (messageText.equals("Вернуться в меню")) {
                // Возврат в главное меню
                user.setLastState(null);
                userDao.save(user);
                sendMessage(vk, groupActor, userId.intValue(), "Вы вернулись в главное меню.");
                sendDefaultKeyboard(vk, groupActor, userId.intValue());
                return;
            }
            
            // Обработка сообщения, если юзер в режиме контакта с админом и не является админом
            if (userState != null && userState.equals("CONTACTING_ADMIN") && !isAdmin) {
                // Здесь можно добавить отправку сообщения админу
                int adminId = AppConfig.getInstance().getAdminUserId();
                if (adminId > 0) {
                    try {
                        sendMessage(vk, groupActor, adminId, "Сообщение от пользователя " + userId + ":\n\n" + messageText);
                        sendMessage(vk, groupActor, userId.intValue(), "Ваше сообщение отправлено администратору. Ожидайте ответа.");
                    } catch (Exception e) {
                        logger.error("Ошибка при отправке сообщения администратору: {}", e.getMessage());
                        sendMessage(vk, groupActor, userId.intValue(), "Ошибка при отправке сообщения администратору.");
                    }
                } else {
                    sendMessage(vk, groupActor, userId.intValue(), "Администратор не настроен в системе.");
                }
                return;
            }
            
            // Обработка криптовалютных кнопок
            if (messageText.startsWith("$")) {
                // Обработка запроса графика криптовалюты из кнопки криптоклавиатуры
                String tokenSymbol = messageText.substring(1);
                // Используем улучшенную функцию с более детальной информацией
                sendCryptoInfo(userId, tokenSymbol);
                // После отправки информации, отправляем основную клавиатуру
                sendDefaultKeyboard(vk, groupActor, userId.intValue());
                return;
            }
            
            // Обработка команд с клавиатуры
            if (messageText.startsWith("/crypto")) {
                // Обработка запроса графика криптовалюты
                String[] parts = messageText.split(" ");
                if (parts.length > 1) {
                    String tokenSymbol = parts[1].toUpperCase();
                    sendCryptoInfo(userId, tokenSymbol);
                } else {
                    // Отправка сообщения о некорректном формате команды
                    sendMessage(vk, groupActor, userId.intValue(), "Пожалуйста, укажите символ криптовалюты. Например: /crypto BTC");
                }
                return;
            } else if (messageText.equals("/payment_status") || messageText.equals("Инфо о заказе")) {
                // Проверка статуса платежей пользователя
                List<Payment> userPayments = paymentService.getPaymentsByUserId(userId);
                if (userPayments.isEmpty()) {
                    sendMessage(vk, groupActor, userId.intValue(), "У вас нет активных платежей.");
                } else {
                    StringBuilder statusMessage = new StringBuilder("Ваши платежи:\n\n");
                    for (Payment payment : userPayments) {
                        statusMessage.append(String.format(
                            "ID: %d\nСумма: %.2f руб.\nСтатус: %s\nСоздан: %s\n\n",
                            payment.getId(), payment.getAmount(), 
                            getPaymentStatusText(payment.getStatus()), 
                            new SimpleDateFormat("dd.MM.yyyy HH:mm").format(payment.getCreatedAt())
                        ));
                    }
                    sendMessage(vk, groupActor, userId.intValue(), statusMessage.toString());
                }
                return;
            } else if (messageText.equals("/help") || messageText.equals("Помощь")) {
                // Отправка справки по командам
                sendHelpMessage(vk, groupActor, userId.intValue());
                return;
            } else if (messageText.equals("/feedback") || messageText.equals("Оставить отзыв")) {
                // Активируем режим получения обратной связи
                activateFeedbackMode(vk, groupActor, user, userId.intValue());
                return;
            } else if (messageText.equals("Отмена") && userState != null && 
                      (userState.equals("FEEDBACK_RATING") || userState.equals("FEEDBACK_COMMENT"))) {
                // Отмена режима обратной связи
                user.setLastState(null);
                userDao.save(user);
                sendMessage(vk, groupActor, userId.intValue(), "Отзыв отменен. Спасибо за использование бота!");
                sendDefaultKeyboard(vk, groupActor, userId.intValue());
                return;
            } else if (messageText.equals("Оформить заказ") || messageText.equals("🛒 Заказать товар")) {
                // Показываем каталог товаров
                user.setLastState("SELECTING_PRODUCT_CATEGORY");
                userDao.save(user);
                userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_PRODUCT_CATEGORY");
                
                try {
                    // Принудительно отправляем клавиатуру с категориями товаров
                    vk.messages().send(groupActor)
                            .userId(userId.intValue())
                            .message("Выберите категорию товара:")
                            .keyboard(VkKeyboardFactory.getProductCatalogKeyboard())
                            .randomId(random.nextInt(100000))
                            .execute();
                    logger.info("Отправлена клавиатура с категориями товаров пользователю {}", userId);
                } catch (ApiException | ClientException e) {
                    logger.error("Ошибка при отправке клавиатуры: {}", e.getMessage(), e);
                    // В случае ошибки пытаемся отправить через вспомогательный метод
                    Keyboard catalogKeyboard = VkKeyboardFactory.getProductCatalogKeyboard();
                    sendMessageWithKeyboard(vk, groupActor, userId.intValue(), "Выберите категорию товара:", catalogKeyboard);
                }
                return;
            } else if (messageText.equals("Отмена") && "CREATING_ORDER".equals(user.getLastState())) {
                // Отмена создания заказа
                user.setLastState(null);
                userDao.save(user);
                sendDefaultKeyboard(vk, groupActor, userId.intValue());
                return;
            } else if (messageText.equals("Криптовалюты")) {
                // Отправляем клавиатуру с криптовалютами и устанавливаем состояние
                userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "VIEWING_CRYPTO");
                vk.messages().send(groupActor)
                        .message("Выберите интересующую вас криптовалюту для получения информации:")
                        .userId(userId.intValue())
                        .randomId(random.nextInt())
                        .keyboard(VkKeyboardFactory.getCryptoKeyboard())
                        .execute();
                return;
            } else if (messageText.equals("Цифровые товары")) {
                userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_DIGITAL_PRODUCT");
                user.setLastState("SELECTING_DIGITAL_PRODUCT");
                userDao.save(user);
                vk.messages().send(groupActor)
                        .message("Выберите категорию цифрового товара:")
                        .userId(userId.intValue())
                        .randomId(random.nextInt())
                        .keyboard(VkKeyboardFactory.getDigitalProductsKeyboard())
                        .execute();
                return;
            } else if (messageText.equals("Физические товары")) {
                userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_PHYSICAL_PRODUCT");
                user.setLastState("SELECTING_PHYSICAL_PRODUCT");
                userDao.save(user);
                vk.messages().send(groupActor)
                        .message("Выберите категорию физического товара:")
                        .userId(userId.intValue())
                        .randomId(random.nextInt())
                        .keyboard(VkKeyboardFactory.getPhysicalProductsKeyboard())
                        .execute();
                return;
            } else if (messageText.equals("Услуги")) {
                userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_SERVICE");
                user.setLastState("SELECTING_SERVICE");
                userDao.save(user);
                vk.messages().send(groupActor)
                        .message("Выберите категорию услуги:")
                        .userId(userId.intValue())
                        .randomId(random.nextInt())
                        .keyboard(VkKeyboardFactory.getServicesKeyboard())
                        .execute();
                return;
            } else if (messageText.equals("Связаться с админом")) {
                // Установка состояния и отправка инструкций
                user.setLastState("CONTACTING_ADMIN");
                userDao.save(user);
                sendKeyboardForAdminContact(vk, groupActor, userId.intValue());
                return;
            }
            
            // Обработка состояний пользователя
            if (userState != null) {
                // Проверка наличия состояния в userStates для синхронизации
                if (!userStates.containsKey(userId)) {
                    userStates.put(userId, new HashMap<>());
                    userStates.get(userId).put("STATE", userState);
                }
                
                // Синхронизация состояний из оперативной памяти и базы данных
                String inMemoryState = userStates.containsKey(userId) && userStates.get(userId).containsKey("STATE") ? 
                    (String) userStates.get(userId).get("STATE") : null;
                
                if (inMemoryState != null && !inMemoryState.equals(userState)) {
                    // Обновляем состояние в базе данных
                    user.setLastState(inMemoryState);
                    userDao.save(user);
                    userState = inMemoryState;
                }
                
                // Обработка выбора категории товаров
                if (userState.equals("SELECTING_PRODUCT_CATEGORY")) {
                    if (messageText.equals("Цифровые товары")) {
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_DIGITAL_PRODUCT");
                        user.setLastState("SELECTING_DIGITAL_PRODUCT");
                        userDao.save(user);
                        
                        // Принудительно отправляем клавиатуру с цифровыми товарами
                        try {
                            vk.messages().send(groupActor)
                                    .userId(userId.intValue())
                                    .message("Выберите категорию цифрового товара:")
                                    .keyboard(VkKeyboardFactory.getDigitalProductsKeyboard())
                                    .randomId(random.nextInt(100000))
                                    .execute();
                            logger.info("Отправлена клавиатура с цифровыми товарами пользователю {}", userId);
                        } catch (Exception e) {
                            logger.error("Ошибка при отправке клавиатуры с цифровыми товарами: {}", e.getMessage(), e);
                        }
                        return;
                    } else if (messageText.equals("Физические товары")) {
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_PHYSICAL_PRODUCT");
                        user.setLastState("SELECTING_PHYSICAL_PRODUCT");
                        userDao.save(user);
                        
                        // Принудительно отправляем клавиатуру с физическими товарами
                        try {
                            vk.messages().send(groupActor)
                                    .userId(userId.intValue())
                                    .message("Выберите категорию физического товара:")
                                    .keyboard(VkKeyboardFactory.getPhysicalProductsKeyboard())
                                    .randomId(random.nextInt(100000))
                                    .execute();
                            logger.info("Отправлена клавиатура с физическими товарами пользователю {}", userId);
                        } catch (Exception e) {
                            logger.error("Ошибка при отправке клавиатуры с физическими товарами: {}", e.getMessage(), e);
                        }
                        return;
                    } else if (messageText.equals("Услуги")) {
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_SERVICE");
                        user.setLastState("SELECTING_SERVICE");
                        userDao.save(user);
                        
                        // Принудительно отправляем клавиатуру с услугами
                        try {
                            vk.messages().send(groupActor)
                                    .userId(userId.intValue())
                                    .message("Выберите категорию услуги:")
                                    .keyboard(VkKeyboardFactory.getServicesKeyboard())
                                    .randomId(random.nextInt(100000))
                                    .execute();
                            logger.info("Отправлена клавиатура с услугами пользователю {}", userId);
                        } catch (Exception e) {
                            logger.error("Ошибка при отправке клавиатуры с услугами: {}", e.getMessage(), e);
                        }
                        return;
                    } else if (messageText.equals("Отмена")) {
                        user.setLastState(null);
                        userDao.save(user);
                        userStates.remove(userId);
                        sendDefaultKeyboard(vk, groupActor, userId.intValue());
                        return;
                    } else {
                        // Повторно отправляем клавиатуру с категориями товаров
                        try {
                            vk.messages().send(groupActor)
                                    .userId(userId.intValue())
                                    .message("Пожалуйста, выберите категорию товара из предложенных или нажмите 'Отмена':")
                                    .keyboard(VkKeyboardFactory.getProductCatalogKeyboard())
                                    .randomId(random.nextInt(100000))
                                    .execute();
                            logger.info("Повторно отправлена клавиатура с категориями товаров пользователю {}", userId);
                        } catch (Exception e) {
                            logger.error("Ошибка при отправке клавиатуры с категориями товаров: {}", e.getMessage(), e);
                        }
                        return;
                    }
                }
                
                // Обработка выбора цифрового товара
                else if (userState.equals("SELECTING_DIGITAL_PRODUCT")) {
                    if (messageText.equals("Назад")) {
                        user.setLastState("SELECTING_PRODUCT_CATEGORY");
                        userDao.save(user);
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_PRODUCT_CATEGORY");
                        Keyboard catalogKeyboard = VkKeyboardFactory.getProductCatalogKeyboard();
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), "Выберите категорию товара:", catalogKeyboard);
                        return;
                    } else if (messageText.equals("Отмена")) {
                        user.setLastState(null);
                        userDao.save(user);
                        userStates.remove(userId);
                        sendDefaultKeyboard(vk, groupActor, userId.intValue());
                        return;
                    } else if (List.of("Аккаунты VPN", "Подписки", "Ключи Steam", "Программы").contains(messageText)) {
                        // Логика обработки выбора конкретного цифрового товара
                        user.setLastState("CREATING_ORDER");
                        userDao.save(user);
                        String productCategory = "Цифровые товары - " + messageText;
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "CREATING_ORDER");
                        userStates.get(userId).put("product_type", productCategory);
                        
                        vk.messages().send(groupActor)
                                .message("Вы выбрали категорию: " + productCategory + "\nТеперь укажите детали заказа:")
                                .userId(userId.intValue())
                                .randomId(random.nextInt())
                                .keyboard(VkKeyboardFactory.getOrderKeyboard())
                                .execute();
                        return;
                    } else {
                        // Повторно отправляем клавиатуру с цифровыми товарами
                        Keyboard digitalProductsKeyboard = VkKeyboardFactory.getDigitalProductsKeyboard();
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), 
                            "Пожалуйста, выберите товар из предложенных или нажмите 'Назад' для возврата к категориям:", 
                            digitalProductsKeyboard);
                        return;
                    }
                }
                
                // Обработка выбора физического товара
                else if (userState.equals("SELECTING_PHYSICAL_PRODUCT")) {
                    if (messageText.equals("Назад")) {
                        user.setLastState("SELECTING_PRODUCT_CATEGORY");
                        userDao.save(user);
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_PRODUCT_CATEGORY");
                        Keyboard catalogKeyboard = VkKeyboardFactory.getProductCatalogKeyboard();
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), "Выберите категорию товара:", catalogKeyboard);
                        return;
                    } else if (messageText.equals("Отмена")) {
                        user.setLastState(null);
                        userDao.save(user);
                        userStates.remove(userId);
                        sendDefaultKeyboard(vk, groupActor, userId.intValue());
                        return;
                    } else if (List.of("Одежда", "Обувь", "Аксессуары", "Техника").contains(messageText)) {
                        // Логика обработки выбора конкретного физического товара
                        user.setLastState("CREATING_ORDER");
                        userDao.save(user);
                        String productCategory = "Физические товары - " + messageText;
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "CREATING_ORDER");
                        userStates.get(userId).put("product_type", productCategory);
                        
                        vk.messages().send(groupActor)
                                .message("Вы выбрали категорию: " + productCategory + "\nТеперь укажите детали заказа:")
                                .userId(userId.intValue())
                                .randomId(random.nextInt())
                                .keyboard(VkKeyboardFactory.getOrderKeyboard())
                                .execute();
                        return;
                    } else {
                        // Повторно отправляем клавиатуру с физическими товарами
                        Keyboard physicalProductsKeyboard = VkKeyboardFactory.getPhysicalProductsKeyboard();
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), 
                            "Пожалуйста, выберите товар из предложенных или нажмите 'Назад' для возврата к категориям:", 
                            physicalProductsKeyboard);
                        return;
                    }
                }
                
                // Обработка выбора услуги
                else if (userState.equals("SELECTING_SERVICE")) {
                    if (messageText.equals("Назад")) {
                        user.setLastState("SELECTING_PRODUCT_CATEGORY");
                        userDao.save(user);
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "SELECTING_PRODUCT_CATEGORY");
                        Keyboard catalogKeyboard = VkKeyboardFactory.getProductCatalogKeyboard();
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), "Выберите категорию товара:", catalogKeyboard);
                        return;
                    } else if (messageText.equals("Отмена")) {
                        user.setLastState(null);
                        userDao.save(user);
                        userStates.remove(userId);
                        sendDefaultKeyboard(vk, groupActor, userId.intValue());
                        return;
                    } else if (List.of("Консультации", "Обучение", "Дизайн", "IT-услуги").contains(messageText)) {
                        // Логика обработки выбора конкретной услуги
                        user.setLastState("CREATING_ORDER");
                        userDao.save(user);
                        String productCategory = "Услуги - " + messageText;
                        userStates.computeIfAbsent(userId, k -> new HashMap<>()).put("STATE", "CREATING_ORDER");
                        userStates.get(userId).put("product_type", productCategory);
                        
                        vk.messages().send(groupActor)
                                .message("Вы выбрали категорию: " + productCategory + "\nТеперь укажите детали заказа:")
                                .userId(userId.intValue())
                                .randomId(random.nextInt())
                                .keyboard(VkKeyboardFactory.getOrderKeyboard())
                                .execute();
                        return;
                    } else {
                        // Повторно отправляем клавиатуру с услугами
                        Keyboard servicesKeyboard = VkKeyboardFactory.getServicesKeyboard();
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), 
                            "Пожалуйста, выберите услугу из предложенных или нажмите 'Назад' для возврата к категориям:", 
                            servicesKeyboard);
                        return;
                    }
                }
                
                // Обработка оценок
                else if (userState.equals("FEEDBACK_RATING")) {
                    // Проверяем, является ли сообщение оценкой
                    int rating = 0;
                    if (messageText.matches("⭐+")) {
                        rating = messageText.length(); // Количество звезд = оценка
                        
                        // Здесь можно сохранить оценку в базе данных
                        // Для примера, просто логируем
                        logger.info("Получена оценка {} от пользователя {}", rating, userId);
                        
                        // Запрашиваем текстовый комментарий
                        user.setLastState("FEEDBACK_COMMENT");
                        userDao.save(user);
                        
                        Keyboard cancelKeyboard = new Keyboard();
                        List<List<KeyboardButton>> buttons = new ArrayList<>();
                        List<KeyboardButton> row = new ArrayList<>();
                        row.add(new KeyboardButton()
                                .setAction(new KeyboardButtonAction()
                                .setType(TemplateActionTypeNames.TEXT)
                                .setLabel("Пропустить"))
                                .setColor(KeyboardButtonColor.DEFAULT));
                        row.add(new KeyboardButton()
                                .setAction(new KeyboardButtonAction()
                                .setType(TemplateActionTypeNames.TEXT)
                                .setLabel("Отмена"))
                                .setColor(KeyboardButtonColor.NEGATIVE));
                        buttons.add(row);
                        cancelKeyboard.setButtons(buttons);
                        cancelKeyboard.setOneTime(true);
                        
                        sendMessageWithKeyboard(vk, groupActor, userId.intValue(), 
                                              "Спасибо за оценку! Хотите оставить текстовый комментарий?", 
                                              cancelKeyboard);
                        return;
                    } else {
                        sendMessage(vk, groupActor, userId.intValue(), "Пожалуйста, выберите оценку, нажав на соответствующую кнопку.");
                        return;
                    }
                } 
                // Обработка текстового отзыва
                else if (userState.equals("FEEDBACK_COMMENT")) {
                    if (messageText.equals("Пропустить")) {
                        // Пропускаем комментарий
                        user.setLastState(null);
                        userDao.save(user);
                        sendMessage(vk, groupActor, userId.intValue(), "Спасибо за ваш отзыв! Это помогает нам становиться лучше.");
                        sendDefaultKeyboard(vk, groupActor, userId.intValue());
                        return;
                    }
                    
                    // Сохраняем комментарий (здесь можно добавить сохранение в БД)
                    logger.info("Получен комментарий от пользователя {}: {}", userId, messageText);
                    
                    // Отправляем сообщение администратору
                    int adminId = AppConfig.getInstance().getAdminUserId();
                    if (adminId > 0) {
                        try {
                            sendMessage(vk, groupActor, adminId, 
                                      String.format("📋 Новый отзыв от пользователя %d:\n\n%s", userId, messageText));
                        } catch (Exception e) {
                            logger.error("Ошибка при отправке отзыва администратору: {}", e.getMessage());
                        }
                    }
                    
                    // Сбрасываем состояние и благодарим пользователя
                    user.setLastState(null);
                    userDao.save(user);
                    sendMessage(vk, groupActor, userId.intValue(), 
                              "Большое спасибо за ваш отзыв! Мы внимательно изучим ваше мнение и постараемся стать еще лучше.");
                    sendDefaultKeyboard(vk, groupActor, userId.intValue());
                    return;
                }
                // Обработка создания заказа
                else if (userState.equals("CREATING_ORDER")) {
                    // Добавляем информацию о товаре к заказу, если она есть
                    Map<String, Object> state = userStates.computeIfAbsent(userId, k -> new HashMap<>());
                    String orderText = messageText;
                    
                    if (state.containsKey("product_type")) {
                        String productType = (String) state.get("product_type");
                        orderText = "Товар: " + productType + "\n" + orderText;
                    }
                    
                    // Создание заказа
                    try {
                        Long orderId = orderDao.saveOrder(userId, orderText, "NEW");
                        Order newOrder = new Order(userId, orderText);
                        newOrder.setOrderText("Заказ #" + orderId + ": " + orderText);
                        
                        // Создание платежа для заказа после успешного создания заказа
                        double orderAmount = calculateOrderAmount(orderText);
                        java.math.BigDecimal amount = new java.math.BigDecimal(orderAmount);
                        String paymentComment = "ORD" + orderId;
                        
                        Payment payment = paymentService.createPayment(userId, String.valueOf(orderId), amount, paymentComment);
                        
                        // Сброс состояния пользователя и очистка временных данных
                        user.setLastState(null);
                        userDao.save(user);
                        userStates.remove(userId);
                        
                        // Отправка инструкций по оплате
                        sendPaymentInstructions(vk, groupActor, userId.intValue(), payment);
                        return;
                    } catch (Exception e) {
                        logger.error("Ошибка при создании заказа/платежа: {}", e.getMessage(), e);
                        sendMessage(vk, groupActor, userId.intValue(), "Произошла ошибка при оформлении заказа. Пожалуйста, попробуйте позже.");
                        
                        // Сброс состояния пользователя в случае ошибки
                        user.setLastState(null);
                        userDao.save(user);
                        userStates.remove(userId);
                        return;
                    }
                }
            }
            
            // Обработка команд для подписки на уведомления о криптовалютах
            if (messageText.startsWith("/подписка") || messageText.startsWith("/subscribe")) {
                handleCryptoSubscription(vk, groupActor, userId.intValue(), messageText);
                return;
            }
            
            // Обработка команд для отписки от уведомлений о криптовалютах
            if (messageText.startsWith("/отписка") || messageText.startsWith("/unsubscribe")) {
                handleCryptoUnsubscription(vk, groupActor, userId.intValue(), messageText);
                return;
            }
            
            // Обработка команды для просмотра подписок
            if (messageText.equals("/мои_подписки") || messageText.equals("/my_subscriptions")) {
                listUserSubscriptions(vk, groupActor, userId.intValue());
                return;
            }
            
            // Стандартный ответ, если ничего не подошло
            if (userState == null || userState.isEmpty()) {
                sendDefaultKeyboard(vk, groupActor, userId.intValue());
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при обработке сообщения пользователя: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Загрузка фото в ВК и получение строки для attachment из байтов изображения
     */
    private static String uploadPhotoToVK(GroupActor groupActor, VkApiClient vk, byte[] imageBytes) throws ClientException, ApiException, IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Изображение отсутствует или пустое");
        }
        
        // Создаем временный файл из байтов
        File tempFile = File.createTempFile("vk_upload_", ".png");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(imageBytes);
        }
        
        try {
            // Получаем строку attachment из файла
            return uploadPhotoToVK(groupActor, vk, tempFile);
        } finally {
            // Удаляем временный файл
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * Загрузка фото в ВК и получение строки для attachment из файла
     */
    private static String uploadPhotoToVK(GroupActor groupActor, VkApiClient vk, File image) throws ClientException, ApiException, IOException {
        if (image == null || !image.exists() || image.length() == 0) {
            throw new IllegalArgumentException("Файл изображения отсутствует или пустой");
        }
        
        // Получаем URL для загрузки изображения
        URI uploadUrlUri = vk.photos().getMessagesUploadServer(groupActor).execute().getUploadUrl();
        String uploadUrl = uploadUrlUri.toString();
        
        // Загружаем изображение на сервер
        MessageUploadResponse uploadResponse = vk.upload().photoMessage(uploadUrl, image).execute();
        
        // Сохраняем загруженное изображение
        List<SaveMessagesPhotoResponse> savedPhotos = vk.photos().saveMessagesPhoto(groupActor, uploadResponse.getPhoto())
                .server(uploadResponse.getServer())
                .hash(uploadResponse.getHash())
                .execute();
        
        if (savedPhotos.isEmpty()) {
            throw new IOException("Не удалось сохранить загруженное фото");
        }
        
        SaveMessagesPhotoResponse savedPhoto = savedPhotos.get(0);
        // Возвращаем строку для attachment в формате photo{owner_id}_{photo_id}
        return "photo" + savedPhoto.getOwnerId() + "_" + savedPhoto.getId();
    }
    
    /**
     * Конвертирует модель TokenChartResult в внутренний класс TokenChartService.TokenChartResult
     * Это нужно для обратной совместимости при использовании метода createTokenChart
     */
    private static TokenChartService.TokenChartResult convertToInternalResult(ru.escapismart.model.TokenChartResult result) throws IOException {
        if (result == null) {
            return new TokenChartService.TokenChartResult("Не удалось получить данные о токене");
        }
        
        if (!result.isSuccess() || result.getImageBytes() == null || result.getImageBytes().length == 0) {
            return new TokenChartService.TokenChartResult(result.getErrorMessage() != null ? 
                    result.getErrorMessage() : "Не удалось получить график токена");
        }
        
        // Создаем временный файл из байтов изображения
        File tempFile = File.createTempFile("chart_", ".png");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(result.getImageBytes());
        }
        
        return new TokenChartService.TokenChartResult(tempFile);
    }
    
    /**
     * Отправляет информацию о криптовалюте пользователю.
     * @param userId ID пользователя
     * @param token Символ токена (например, BTC)
     */
    private static void sendCryptoInfo(Long userId, String token) {
        try {
            logger.info("Запрос на отправку информации о токене {} пользователю {}", token, userId);
            
            // Отправляем сообщение о загрузке данных
            sendMessage(vk, groupActor, userId.intValue(), "Получаю информацию о " + token.toUpperCase() + "...");
            
            try {
                // Получаем полную информацию о токене (график и метаданные)
                ru.escapismart.model.TokenChartResult tokenInfo = tokenChartService.getTokenChartResult(token);
                
                if (tokenInfo != null && tokenInfo.isSuccess() && tokenInfo.getImageBytes() != null) {
                    // Загружаем изображение графика в VK и получаем ID
                    String photoId = uploadPhotoToVK(groupActor, vk, tokenInfo.getImageBytes());
                    
                    // Формируем сообщение с информацией о токене
                    StringBuilder message = new StringBuilder();
                    message.append("📊 Информация о ").append(token.toUpperCase()).append(":\n\n");
                    message.append("💰 Текущая цена: $").append(String.format("%.2f", tokenInfo.getCurrentPrice())).append("\n");
                    
                    // Форматируем изменение цены за 24 часа
                    double priceChange = tokenInfo.getChange24h();
                    String changeEmoji = priceChange >= 0 ? "📈" : "📉";
                    String changeSign = priceChange >= 0 ? "+" : "";
                    
                    message.append(changeEmoji).append(" Изменение за 24ч: ")
                           .append(changeSign).append(String.format("%.2f", priceChange)).append("%\n\n");
                    
                    message.append("🔗 Подробнее: ").append(tokenInfo.getCmcLink());
                    
                    // Отправляем сообщение с графиком
                    vk.messages()
                            .send(groupActor)
                            .userId(userId.intValue())
                            .message(message.toString())
                            .attachment(photoId)
                            .randomId(random.nextInt())
                            .execute();
                    
                    logger.info("Информация о токене {} успешно отправлена пользователю {}", token, userId);
                } else {
                    logger.warn("Получены некорректные данные токена из getTokenChartResult: {}", tokenInfo);
                    throw new Exception("Некорректные данные токена");
                }
            } catch (Exception e) {
                logger.error("Ошибка при отправке информации о токене {}: {}", token, e.getMessage(), e);
                
                // Пробуем использовать старый метод как запасной вариант
                TokenChartService.TokenChartResult result = tokenChartService.createTokenChart(token);
                if (result.isSuccess()) {
                    try {
                        // Загружаем изображение графика в VK и получаем ID
                        String photoId = uploadPhotoToVK(groupActor, vk, result.getChartFile());
                        
                        vk.messages()
                                .send(groupActor)
                                .userId(userId.intValue())
                                .message("📊 Информация о " + token.toUpperCase() + 
                                        " (базовая версия):\n\n🔗 Подробнее: https://coinmarketcap.com/currencies/" + token.toLowerCase())
                                .attachment(photoId)
                                .randomId(random.nextInt())
                                .execute();
                        
                        logger.info("Отправлена ограниченная информация о токене {} пользователю {}", token, userId);
                    } catch (Exception uploadEx) {
                        logger.error("Ошибка при загрузке изображения: {}", uploadEx.getMessage(), uploadEx);
                        sendMessage(vk, groupActor, userId.intValue(), 
                                "Извините, не удалось загрузить график для " + token.toUpperCase() + ". Пожалуйста, попробуйте позже.");
                    }
                } else {
                    sendMessage(vk, groupActor, userId.intValue(), 
                            "Извините, не удалось получить информацию о " + token.toUpperCase() + ": " + result.getErrorMessage());
                    logger.error("Не удалось создать график для {}: {}", token, result.getErrorMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Критическая ошибка при обработке запроса информации о токене {}: {}", token, e.getMessage(), e);
            try {
                sendMessage(vk, groupActor, userId.intValue(), 
                        "Произошла ошибка при получении данных о " + token.toUpperCase() + ". Пожалуйста, попробуйте позже.");
            } catch (Exception ex) {
                logger.error("Не удалось отправить сообщение об ошибке: {}", ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Отправляет инструкции по оплате
     */
    private static void sendPaymentInstructions(VkApiClient vk, GroupActor groupActor, 
                                               int userId, Payment payment) {
        try {
            String message = String.format(
                "Заказ создан! Для оплаты переведите %.2f руб. на карту:\n\n" +
                "%s\n\n" + 
                "В комментарии к переводу укажите: %s\n\n" +
                "После оплаты проверьте статус командой /payment_status",
                payment.getAmount(),
                AppConfig.getInstance().getPaymentAccount(),
                payment.getPaymentComment()
            );
            
            sendMessage(vk, groupActor, userId, message);
        } catch (Exception e) {
            logger.error("Ошибка при отправке инструкций по оплате: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Преобразование статуса платежа в текст
     */
    private static String getPaymentStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "Ожидает оплаты";
            case "COMPLETED":
                return "Оплачен";
            case "CANCELLED":
                return "Отменен";
            default:
                return "Неизвестный статус";
        }
    }
    
    /**
     * Расчет суммы заказа
     */
    private static double calculateOrderAmount(String orderText) {
        // Здесь должна быть логика расчета стоимости заказа
        // Для примера используем простой алгоритм
        return 100.0 + (orderText.length() * 2.5);
    }
    
    private static void sendMessage(VkApiClient vk, GroupActor groupActor, int userId, String message) throws ApiException, ClientException {
        vk.messages().send(groupActor)
                .userId(userId)
                .message(message)
                .randomId(random.nextInt(100000))
                .execute();
        
        logger.info("Отправлено сообщение пользователю {}: {}", userId, message.substring(0, Math.min(message.length(), 50)) + (message.length() > 50 ? "..." : ""));
    }

    /**
     * Отправка приветственного сообщения
     */
    private static void sendWelcomeMessage(VkApiClient vk, GroupActor groupActor, int userId) {
        try {
            String message = "👋 Добро пожаловать в VK Escapism Bot!\n\n" +
                             "С помощью этого бота вы можете:\n" +
                             "📈 Получать актуальные графики и данные по криптовалютам\n" +
                             "🛒 Создавать и оформлять заказы\n" +
                             "💳 Отслеживать статус платежей\n" +
                             "📞 Связаться с администратором\n\n" +
                             "Используйте кнопки ниже для навигации.";
            
            Keyboard keyboard = VkKeyboardFactory.getMainKeyboard();
            
            vk.messages().send(groupActor)
                .userId(userId)
                .message(message)
                .keyboard(keyboard)
                .randomId(random.nextInt(100000))
                .execute();
            
            logger.info("Отправлено приветственное сообщение пользователю {}", userId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке приветственного сообщения: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправка справочного сообщения
     */
    private static void sendHelpMessage(VkApiClient vk, GroupActor groupActor, int userId) {
        try {
            StringBuilder helpMessage = new StringBuilder();
            helpMessage.append("📚 Доступные команды:\n\n");
            
            // Основные команды
            helpMessage.append("💼 Заказы:\n");
            helpMessage.append("- Новый заказ - создание нового заказа\n");
            helpMessage.append("- Мои заказы - просмотр статуса заказов\n\n");
            
            // Криптовалютные команды
            helpMessage.append("💹 Криптовалюты:\n");
            helpMessage.append("- $[символ] - информация о криптовалюте (например, $BTC)\n");
            helpMessage.append("- /подписка СИМВОЛ [порог%] - подписка на уведомления о изменении цены\n");
            helpMessage.append("- /отписка СИМВОЛ - отписка от уведомлений\n");
            helpMessage.append("- /мои_подписки - просмотр ваших подписок\n\n");
            
            // Связь с админом
            helpMessage.append("👨‍💼 Связь с администратором:\n");
            helpMessage.append("- Связаться с админом - режим чата с администратором\n\n");
            
            // Прочие команды
            helpMessage.append("ℹ️ Прочее:\n");
            helpMessage.append("- Помощь - показать эту справку\n");
            helpMessage.append("- Начать - сбросить состояние и начать с начала\n");
            
            // Отправляем сообщение
            sendMessage(vk, groupActor, userId, helpMessage.toString());
            
        } catch (ApiException | ClientException e) {
            logger.error("Ошибка при отправке справки: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Создает клавиатуру для справки с кнопкой обратной связи
     */
    private static Keyboard createFeedbackKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Оставить отзыв"))
                .setColor(KeyboardButtonColor.POSITIVE));
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Вернуться в меню"))
                .setColor(KeyboardButtonColor.PRIMARY));
        
        allButtons.add(row1);
        allButtons.add(row2);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(false);
        
        return keyboard;
    }
    
    /**
     * Активирует режим обратной связи для пользователя
     */
    private static void activateFeedbackMode(VkApiClient vk, GroupActor groupActor, User user, int userId) {
        try {
            String message = "⭐ Оцените работу бота от 1 до 5, где 5 - отлично, 1 - плохо.\n\n" +
                             "Вы также можете оставить текстовый комментарий после оценки.";
            
            // Создаем клавиатуру с оценками
            Keyboard ratingKeyboard = createRatingKeyboard();
            
            // Устанавливаем состояние пользователя в режим обратной связи
            user.setLastState("FEEDBACK_RATING");
            
            vk.messages().send(groupActor)
                .userId(userId)
                .message(message)
                .keyboard(ratingKeyboard)
                .randomId(random.nextInt(100000))
                .execute();
            
            logger.info("Активирован режим обратной связи для пользователя {}", userId);
        } catch (Exception e) {
            logger.error("Ошибка при активации режима обратной связи: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Создает клавиатуру с оценками от 1 до 5
     */
    private static Keyboard createRatingKeyboard() {
        Keyboard keyboard = new Keyboard();
        List<List<KeyboardButton>> allButtons = new ArrayList<>();
        
        List<KeyboardButton> row1 = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String label = "";
            for (int j = 0; j < i; j++) {
                label += "⭐";
            }
            row1.add(new KeyboardButton()
                    .setAction(new KeyboardButtonAction()
                    .setType(TemplateActionTypeNames.TEXT)
                    .setLabel(label))
                    .setColor(KeyboardButtonColor.DEFAULT));
        }
        
        List<KeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton()
                .setAction(new KeyboardButtonAction()
                .setType(TemplateActionTypeNames.TEXT)
                .setLabel("Отмена"))
                .setColor(KeyboardButtonColor.NEGATIVE));
        
        allButtons.add(row1);
        allButtons.add(row2);
        
        keyboard.setButtons(allButtons);
        keyboard.setOneTime(true);
        
        return keyboard;
    }

    /**
     * Отправка основной клавиатуры
     */
    private static void sendDefaultKeyboard(VkApiClient vk, GroupActor groupActor, int userId) {
        try {
            String message = "Выберите действие:";
            
            // Создаем новую клавиатуру каждый раз
            Keyboard keyboard = VkKeyboardFactory.getMainKeyboard();
            
            // Прямой вызов VK API для отправки сообщения с клавиатурой
            vk.messages().send(groupActor)
                .userId(userId)
                .message(message)
                .keyboard(keyboard)
                .randomId(random.nextInt(100000))
                .execute();
            
            // Сбрасываем состояние пользователя для синхронизации
            User user = userDao.getUser(Long.valueOf(userId));
            if (user != null && user.getLastState() != null) {
                user.setLastState(null);
                userDao.save(user);
            }
            // Очищаем состояние пользователя из оперативной памяти
            userStates.remove(Long.valueOf(userId));
            
            logger.info("Отправлена основная клавиатура пользователю {}", userId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке клавиатуры: {}", e.getMessage(), e);
            // Если не получилось отправить клавиатуру, отправляем просто сообщение
            try {
                vk.messages().send(groupActor)
                    .userId(userId)
                    .message("Произошла ошибка при отправке клавиатуры. Пожалуйста, напишите /menu для попытки восстановления.")
                    .randomId(random.nextInt(100000))
                    .execute();
            } catch (Exception ex) {
                logger.error("Критическая ошибка при отправке сообщения об ошибке: {}", ex.getMessage(), ex);
            }
        }
    }

    private static void sendMessageWithKeyboard(VkApiClient vk, GroupActor groupActor, int userId, String message, Keyboard keyboard) throws ApiException, ClientException {
        vk.messages().send(groupActor)
                .userId(userId)
                .message(message)
                .keyboard(keyboard)
                .randomId(random.nextInt(100000))
                .execute();
        
        logger.info("Отправлено сообщение с клавиатурой пользователю {}: {}", userId, message);
    }

    /**
     * Отправляет клавиатуру для связи с администратором
     */
    private static void sendKeyboardForAdminContact(VkApiClient vk, GroupActor groupActor, int userId) {
        try {
            // Создаем клавиатуру с кнопкой "Вернуться в меню"
            Keyboard keyboard = new Keyboard();
            List<List<KeyboardButton>> allButtons = new ArrayList<>();
            
            List<KeyboardButton> row1 = new ArrayList<>();
            row1.add(new KeyboardButton().setAction(
                    new KeyboardButtonAction().setLabel("Вернуться в меню").setType(TemplateActionTypeNames.TEXT)
            ).setColor(KeyboardButtonColor.PRIMARY));
            
            allButtons.add(row1);
            keyboard.setButtons(allButtons);
            
            // Отправляем сообщение с клавиатурой
            String message = "Напишите ваше сообщение для администратора. Оно будет отправлено сразу после получения.\n" +
                             "Чтобы вернуться в главное меню, нажмите кнопку ниже.";
            
            sendMessageWithKeyboard(vk, groupActor, userId, message, keyboard);
            
            logger.info("Отправлена клавиатура для связи с администратором пользователю {}", userId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке клавиатуры для связи с администратором: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка команды подписки на уведомления о криптовалютах
     */
    private static void handleCryptoSubscription(VkApiClient vk, GroupActor groupActor, Integer userId, String messageText) {
        try {
            // Формат команды: /подписка СИМВОЛ [порог_изменения]
            String[] parts = messageText.split("\\s+");
            
            if (parts.length < 2) {
                sendMessage(vk, groupActor, userId, 
                    "❌ Неверный формат команды. Используйте:\n" +
                    "/подписка СИМВОЛ [порог_изменения]\n\n" +
                    "Например: /подписка BTC 5\n" +
                    "Это подпишет вас на уведомления при изменении цены Bitcoin на 5% и более.");
                return;
            }
            
            String tokenSymbol = parts[1].toUpperCase();
            double threshold = 5.0; // Порог по умолчанию - 5%
            
            // Если указан порог, используем его
            if (parts.length >= 3) {
                try {
                    threshold = Double.parseDouble(parts[2]);
                    if (threshold <= 0) {
                        threshold = 5.0;
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем ошибку, используем значение по умолчанию
                }
            }
            
            // Проверяем, поддерживается ли токен
            if (!tokenChartService.isSupportedToken(tokenSymbol)) {
                sendMessage(vk, groupActor, userId, 
                    "❌ Токен " + tokenSymbol + " не поддерживается. " +
                    "Пожалуйста, выберите один из поддерживаемых токенов.");
                return;
            }
            
            // Создаем подписку
            boolean success = cryptoAlertService.subscribeToToken(userId.longValue(), tokenSymbol, threshold);
            
            if (success) {
                sendMessage(vk, groupActor, userId, 
                    "✅ Вы успешно подписались на уведомления о изменении цены " + tokenSymbol + ".\n" +
                    "Вы будете получать уведомления при изменении цены на " + threshold + "% и более.");
            } else {
                sendMessage(vk, groupActor, userId, 
                    "❌ Не удалось оформить подписку. Пожалуйста, попробуйте позже.");
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при обработке подписки: {}", e.getMessage(), e);
            try {
                sendMessage(vk, groupActor, userId, "❌ Произошла ошибка при обработке команды.");
            } catch (ApiException | ClientException ex) {
                logger.error("Ошибка при отправке сообщения об ошибке: {}", ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Обработка команды отписки от уведомлений о криптовалютах
     */
    private static void handleCryptoUnsubscription(VkApiClient vk, GroupActor groupActor, Integer userId, String messageText) {
        try {
            // Формат команды: /отписка СИМВОЛ
            String[] parts = messageText.split("\\s+");
            
            if (parts.length < 2) {
                sendMessage(vk, groupActor, userId, 
                    "❌ Неверный формат команды. Используйте:\n" +
                    "/отписка СИМВОЛ\n\n" +
                    "Например: /отписка BTC");
                return;
            }
            
            String tokenSymbol = parts[1].toUpperCase();
            
            // Отменяем подписку
            boolean success = cryptoAlertService.unsubscribeFromToken(userId.longValue(), tokenSymbol);
            
            if (success) {
                sendMessage(vk, groupActor, userId, 
                    "✅ Вы успешно отписались от уведомлений о изменении цены " + tokenSymbol + ".");
            } else {
                sendMessage(vk, groupActor, userId, 
                    "❌ Не удалось отменить подписку. Возможно, вы не были подписаны на этот токен.");
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при обработке отписки: {}", e.getMessage(), e);
            try {
                sendMessage(vk, groupActor, userId, "❌ Произошла ошибка при обработке команды.");
            } catch (ApiException | ClientException ex) {
                logger.error("Ошибка при отправке сообщения об ошибке: {}", ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Отображение списка подписок пользователя
     */
    private static void listUserSubscriptions(VkApiClient vk, GroupActor groupActor, Integer userId) {
        try {
            List<CryptoAlert> subscriptions = cryptoAlertService.getUserSubscriptions(userId.longValue());
            
            if (subscriptions.isEmpty()) {
                sendMessage(vk, groupActor, userId, 
                    "У вас нет активных подписок на уведомления о изменении цен криптовалют.\n\n" +
                    "Чтобы подписаться, используйте команду:\n" +
                    "/подписка СИМВОЛ [порог_изменения]\n\n" +
                    "Например: /подписка BTC 5");
                return;
            }
            
            StringBuilder message = new StringBuilder("📋 Ваши активные подписки:\n\n");
            
            for (CryptoAlert alert : subscriptions) {
                message.append("• ").append(alert.getTokenSymbol())
                      .append(" - порог изменения: ").append(alert.getThreshold()).append("%");
                
                if (alert.getLastPrice() > 0) {
                    message.append(" (посл. цена: $").append(String.format("%.2f", alert.getLastPrice())).append(")");
                }
                
                message.append("\n");
            }
            
            message.append("\nДля отписки используйте команду:\n/отписка СИМВОЛ");
            
            sendMessage(vk, groupActor, userId, message.toString());
            
        } catch (Exception e) {
            logger.error("Ошибка при получении списка подписок: {}", e.getMessage(), e);
            try {
                sendMessage(vk, groupActor, userId, "❌ Произошла ошибка при получении списка подписок.");
            } catch (ApiException | ClientException ex) {
                logger.error("Ошибка при отправке сообщения об ошибке: {}", ex.getMessage(), ex);
            }
        }
    }
} 