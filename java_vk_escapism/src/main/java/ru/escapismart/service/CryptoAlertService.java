package ru.escapismart.service;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.config.AppConfig;
import ru.escapismart.dao.CryptoAlertDao;
import ru.escapismart.model.CryptoAlert;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для мониторинга цен криптовалют и отправки уведомлений
 */
public class CryptoAlertService {
    private static final Logger logger = LoggerFactory.getLogger(CryptoAlertService.class);
    private static CryptoAlertService instance;
    
    private final AppConfig config;
    private final TokenChartService tokenChartService;
    private final CryptoAlertDao cryptoAlertDao;
    private final Map<String, Double> currentPrices;
    private final ScheduledExecutorService scheduler;
    
    private VkApiClient vkClient;
    private GroupActor groupActor;
    private boolean isRunning;
    
    /**
     * Приватный конструктор (Singleton)
     */
    private CryptoAlertService() {
        config = AppConfig.getInstance();
        tokenChartService = TokenChartService.getInstance();
        cryptoAlertDao = new CryptoAlertDao();
        currentPrices = new ConcurrentHashMap<>();
        scheduler = Executors.newScheduledThreadPool(1);
        isRunning = false;
        
        logger.info("CryptoAlertService инициализирован");
    }
    
    /**
     * Получить экземпляр сервиса
     */
    public static synchronized CryptoAlertService getInstance() {
        if (instance == null) {
            instance = new CryptoAlertService();
        }
        return instance;
    }
    
    /**
     * Настройка VK API клиента
     */
    public void setVkClient(VkApiClient vkClient, GroupActor groupActor) {
        this.vkClient = vkClient;
        this.groupActor = groupActor;
    }
    
    /**
     * Запуск мониторинга цен
     */
    public void startMonitoring() {
        if (isRunning) {
            logger.warn("Мониторинг цен уже запущен");
            return;
        }
        
        if (vkClient == null || groupActor == null) {
            logger.error("VK API клиент не настроен. Запуск мониторинга невозможен");
            return;
        }
        
        // Запускаем задачу проверки цен каждые 10 минут
        scheduler.scheduleAtFixedRate(this::checkPrices, 0, 10, TimeUnit.MINUTES);
        isRunning = true;
        
        logger.info("Мониторинг цен криптовалют запущен");
    }
    
    /**
     * Остановка мониторинга
     */
    public void stopMonitoring() {
        if (!isRunning) {
            return;
        }
        
        scheduler.shutdown();
        isRunning = false;
        
        logger.info("Мониторинг цен криптовалют остановлен");
    }
    
    /**
     * Проверка цен и отправка уведомлений
     */
    private void checkPrices() {
        try {
            logger.info("Начинаем проверку цен криптовалют");
            
            // Получаем все активные подписки
            List<CryptoAlert> alerts = cryptoAlertDao.getAllActive();
            if (alerts.isEmpty()) {
                logger.info("Активных подписок на уведомления не найдено");
                return;
            }
            
            // Группируем подписки по токенам для минимизации запросов к API
            Map<String, List<CryptoAlert>> tokenAlerts = new java.util.HashMap<>();
            alerts.forEach(alert -> {
                String tokenSymbol = alert.getTokenSymbol();
                if (!tokenAlerts.containsKey(tokenSymbol)) {
                    tokenAlerts.put(tokenSymbol, new java.util.ArrayList<>());
                }
                tokenAlerts.get(tokenSymbol).add(alert);
            });
            
            // Проверяем цены для каждого токена
            for (Map.Entry<String, List<CryptoAlert>> entry : tokenAlerts.entrySet()) {
                String tokenSymbol = entry.getKey();
                List<CryptoAlert> tokenSubscriptions = entry.getValue();
                
                try {
                    // Получаем текущую цену токена
                    double currentPrice = getCurrentPrice(tokenSymbol);
                    if (currentPrice <= 0) {
                        logger.warn("Не удалось получить цену для токена: {}", tokenSymbol);
                        continue;
                    }
                    
                    // Обрабатываем все подписки на этот токен
                    for (CryptoAlert alert : tokenSubscriptions) {
                        processAlert(alert, currentPrice);
                    }
                    
                } catch (Exception e) {
                    logger.error("Ошибка при обработке подписок для токена {}: {}", 
                            tokenSymbol, e.getMessage(), e);
                }
            }
            
            logger.info("Проверка цен криптовалют завершена");
            
        } catch (Exception e) {
            logger.error("Ошибка при выполнении проверки цен: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Обработка отдельной подписки
     */
    private void processAlert(CryptoAlert alert, double currentPrice) {
        try {
            // Если это первое измерение цены, просто сохраняем текущую цену
            if (alert.getLastPrice() == 0) {
                alert.updatePrice(currentPrice, false);
                cryptoAlertDao.save(alert);
                return;
            }
            
            // Проверяем, нужно ли отправить уведомление
            if (alert.shouldNotify(currentPrice)) {
                // Вычисляем процент изменения
                double percentChange = (currentPrice - alert.getLastPrice()) / alert.getLastPrice() * 100;
                boolean isIncrease = percentChange > 0;
                
                // Формируем текст уведомления
                String message = String.format(
                    "⚠️ Уведомление о изменении цены\n\n" +
                    "Токен: %s\n" +
                    "Предыдущая цена: $%.2f\n" +
                    "Текущая цена: $%.2f\n" +
                    "%s на %.2f%% %s",
                    alert.getTokenSymbol(),
                    alert.getLastPrice(),
                    currentPrice,
                    isIncrease ? "Вырос" : "Упал",
                    Math.abs(percentChange),
                    isIncrease ? "📈" : "📉"
                );
                
                // Отправляем уведомление
                sendMessage(alert.getUserId().intValue(), message);
                
                // Обновляем данные в базе
                alert.updatePrice(currentPrice, true);
                cryptoAlertDao.save(alert);
                
                logger.info("Отправлено уведомление пользователю {} о изменении цены {}",
                        alert.getUserId(), alert.getTokenSymbol());
            } else {
                // Обновляем цену если прошло более 2 часов с последнего обновления
                if (alert.getLastNotification() == null ||
                    java.time.Duration.between(alert.getLastNotification(), java.time.LocalDateTime.now()).toHours() >= 2) {
                    
                    alert.updatePrice(currentPrice, false);
                    cryptoAlertDao.save(alert);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при обработке подписки {}: {}", alert.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Получить текущую цену токена
     */
    private double getCurrentPrice(String tokenSymbol) {
        try {
            // Запрашиваем информацию о токене через TokenChartService
            ru.escapismart.model.TokenChartResult result = tokenChartService.getTokenChartResult(tokenSymbol);
            double price = result.getCurrentPrice();
            
            // Кэшируем цену
            currentPrices.put(tokenSymbol, price);
            
            return price;
        } catch (Exception e) {
            logger.error("Ошибка при получении цены токена {}: {}", tokenSymbol, e.getMessage());
            
            // Если есть кэшированная цена, возвращаем её
            return currentPrices.getOrDefault(tokenSymbol, 0.0);
        }
    }
    
    /**
     * Отправить сообщение пользователю
     */
    private void sendMessage(int userId, String message) {
        try {
            // Генерируем случайный ID для сообщения
            int randomId = (int) (Math.random() * 10000);
            
            // Отправляем сообщение
            vkClient.messages().send(groupActor)
                    .userId(userId)
                    .randomId(randomId)
                    .message(message)
                    .execute();
            
        } catch (ApiException | ClientException e) {
            logger.error("Ошибка при отправке сообщения пользователю {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Создать подписку на токен для пользователя
     */
    public boolean subscribeToToken(long userId, String tokenSymbol, double threshold) {
        try {
            // Проверяем, поддерживается ли токен
            if (!tokenChartService.isSupportedToken(tokenSymbol)) {
                logger.warn("Попытка подписки на неподдерживаемый токен: {}", tokenSymbol);
                return false;
            }
            
            // Создаем или активируем подписку
            boolean result = cryptoAlertDao.activateOrCreate(userId, tokenSymbol, threshold);
            
            if (result) {
                // Запрашиваем текущую цену для инициализации
                CryptoAlert alert = cryptoAlertDao.getByUserAndToken(userId, tokenSymbol);
                if (alert != null && alert.getLastPrice() == 0) {
                    double currentPrice = getCurrentPrice(tokenSymbol);
                    if (currentPrice > 0) {
                        alert.updatePrice(currentPrice, false);
                        cryptoAlertDao.save(alert);
                    }
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при подписке пользователя {} на токен {}: {}", 
                    userId, tokenSymbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Отписаться от уведомлений по токену
     */
    public boolean unsubscribeFromToken(long userId, String tokenSymbol) {
        try {
            return cryptoAlertDao.deactivate(userId, tokenSymbol);
        } catch (Exception e) {
            logger.error("Ошибка при отписке пользователя {} от токена {}: {}", 
                    userId, tokenSymbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получить все активные подписки пользователя
     */
    public List<CryptoAlert> getUserSubscriptions(long userId) {
        return cryptoAlertDao.getActiveByUser(userId);
    }
} 