package ru.escapismart.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс конфигурации приложения.
 * Загружает настройки из файла config.properties и
 * предоставляет безопасный доступ к ним.
 */
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();
    private static AppConfig instance;
    
    // Значения по умолчанию
    private static final String DEFAULT_VK_TOKEN = "vk1.a.DEFAULT_TOKEN_REPLACE_ME";
    private static final int DEFAULT_GROUP_ID = 0;
    
    private static final String DEFAULT_CONFIG_FILE = "app.properties";
    
    // Константы для таймаутов по умолчанию
    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000; // 10 секунд
    private static final int DEFAULT_SOCKET_TIMEOUT = 30000; // 30 секунд
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_BASE_DELAY = 1000; // 1 секунда
    private static final long DEFAULT_RETRY_MAX_DELAY = 10000; // 10 секунд
    
    private static final String PROP_HTTP_CONNECTION_TIMEOUT = "http.connection.timeout";
    private static final String PROP_HTTP_SOCKET_TIMEOUT = "http.socket.timeout";
    private static final String PROP_HTTP_MAX_RETRIES = "http.max.retries";
    private static final String PROP_HTTP_RETRY_BASE_DELAY = "http.retry.base.delay";
    private static final String PROP_HTTP_RETRY_MAX_DELAY = "http.retry.max.delay";
    
    private AppConfig() {
        loadProperties();
    }
    
    public AppConfig(String configFile) {
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            logger.info("Конфигурация загружена из файла: {}", configFile);
        } catch (IOException ex) {
            logger.error("Ошибка при загрузке конфигурации: {}", ex.getMessage());
        }
    }
    
    /**
     * Получить экземпляр конфигурации (Singleton)
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    /**
     * Загрузка свойств из файла config.properties
     */
    private void loadProperties() {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            
            if (input == null) {
                logger.warn("Файл config.properties не найден, используются значения по умолчанию");
                return;
            }
            
            properties.load(input);
            logger.info("Конфигурация успешно загружена");
            
        } catch (IOException ex) {
            logger.error("Ошибка при загрузке файла конфигурации", ex);
        }
    }
    
    /**
     * Получить токен VK API
     */
    public String getVkToken() {
        return properties.getProperty("vk.token", DEFAULT_VK_TOKEN);
    }
    
    /**
     * Получить ID группы VK
     */
    public int getGroupId() {
        try {
            return Integer.parseInt(properties.getProperty("vk.group_id", 
                    String.valueOf(DEFAULT_GROUP_ID)));
        } catch (NumberFormatException e) {
            logger.error("Ошибка при получении ID группы, используется значение по умолчанию", e);
            return DEFAULT_GROUP_ID;
        }
    }
    
    /**
     * Получить URL базы данных
     */
    public String getDatabaseUrl() {
        return properties.getProperty("db.url", "jdbc:postgresql://localhost:5433/vk_escapism");
    }
    
    /**
     * Получить пользователя базы данных
     */
    public String getDatabaseUser() {
        return properties.getProperty("db.user", "postgres");
    }
    
    /**
     * Получить пароль базы данных
     */
    public String getDatabasePassword() {
        return properties.getProperty("db.password", "123");
    }
    
    /**
     * Получить время ожидания для HTTP-запросов (в миллисекундах)
     */
    public int getHttpTimeout() {
        try {
            return Integer.parseInt(properties.getProperty("http.timeout", "10000"));
        } catch (NumberFormatException e) {
            logger.error("Ошибка при получении таймаута HTTP, используется значение по умолчанию", e);
            return 10000;
        }
    }
    
    /**
     * Получить таймаут соединения для HTTP-клиента в миллисекундах
     * @return таймаут соединения
     */
    public int getHttpConnectionTimeout() {
        return getIntProperty("http.connection_timeout", DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * Получить таймаут сокета для HTTP-клиента в миллисекундах
     * @return таймаут сокета
     */
    public int getHttpSocketTimeout() {
        return getIntProperty("http.socket_timeout", DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * Получить максимальное количество повторных попыток для HTTP-запросов
     * @return максимальное количество попыток
     */
    public int getHttpMaxRetries() {
        return getIntProperty("http.max_retries", DEFAULT_MAX_RETRIES);
    }

    /**
     * Получить базовую задержку между повторными попытками в миллисекундах
     * @return базовая задержка
     */
    public long getHttpRetryBaseDelay() {
        return getIntProperty("http.retry_base_delay", (int)DEFAULT_RETRY_BASE_DELAY);
    }

    /**
     * Получить максимальную задержку между повторными попытками в миллисекундах
     * @return максимальная задержка
     */
    public long getHttpRetryMaxDelay() {
        return getIntProperty("http.retry_max_delay", (int)DEFAULT_RETRY_MAX_DELAY);
    }
    
    /**
     * Получить таймаут запроса HTTP (в миллисекундах)
     */
    public int getHttpRequestTimeout() {
        try {
            return Integer.parseInt(properties.getProperty("http.request_timeout", "10000"));
        } catch (NumberFormatException e) {
            logger.error("Ошибка при получении таймаута запроса, используется значение по умолчанию", e);
            return 10000;
        }
    }
    
    /**
     * Получить режим отладки
     */
    public boolean isDebugMode() {
        return Boolean.parseBoolean(properties.getProperty("app.debug_mode", "false"));
    }
    
    /**
     * Получить время кэширования данных о криптовалютах (в минутах)
     */
    public int getCryptoCacheTimeMinutes() {
        try {
            return Integer.parseInt(properties.getProperty("crypto.cache_time_minutes", "5"));
        } catch (NumberFormatException e) {
            logger.error("Ошибка при получении времени кэширования, используется значение по умолчанию", e);
            return 5;
        }
    }
    
    /**
     * Получить время жизни кэша для данных по токенам (в секундах)
     * @return Время жизни кэша в секундах
     */
    public int getTokenCacheExpiryTime() {
        return Integer.parseInt(properties.getProperty("token.cache_expiry_seconds", "300"));
    }
    
    /**
     * Получить API ключ для криптовалютных сервисов
     */
    public String getCryptoApiKey() {
        return properties.getProperty("crypto.api_key", "");
    }
    
    /**
     * Получить альтернативный URL API криптовалют
     */
    public String getAlternativeCryptoApiUrl() {
        return properties.getProperty("crypto.alternative_api_url", "https://api.alternative.com");
    }
    
    /**
     * Использовать ли альтернативный API для криптовалют
     */
    public boolean useAlternativeCryptoApi() {
        return Boolean.parseBoolean(properties.getProperty("crypto.use_alternative_api", "false"));
    }
    
    /**
     * Получить ID администратора для уведомлений
     */
    public int getAdminUserId() {
        try {
            return Integer.parseInt(properties.getProperty("notifications.admin_user_id", "0"));
        } catch (NumberFormatException e) {
            logger.error("Ошибка при получении ID администратора, используется значение по умолчанию", e);
            return 0;
        }
    }
    
    /**
     * Включены ли уведомления об изменении цен
     */
    public boolean isPriceAlertsEnabled() {
        return Boolean.parseBoolean(properties.getProperty("notifications.enable_price_alerts", "false"));
    }
    
    /**
     * Получить порог изменения цены для уведомлений (в процентах)
     */
    public double getPriceChangeThreshold() {
        try {
            return Double.parseDouble(properties.getProperty("notifications.price_change_threshold", "5.0"));
        } catch (NumberFormatException e) {
            logger.error("Ошибка при получении порога изменения цены, используется значение по умолчанию", e);
            return 5.0;
        }
    }
    
    // Получить строковое свойство с значением по умолчанию
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    // Получить целочисленное свойство с значением по умолчанию
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Ошибка при парсинге числового значения для {}: {}", key, e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Получить значение таймаута соединения в миллисекундах
     * @return таймаут соединения
     */
    public int getConnectionTimeout() {
        return getIntProperty("http.connection.timeout", DEFAULT_CONNECTION_TIMEOUT);
    }
    
    /**
     * Получить значение таймаута сокета в миллисекундах
     * @return таймаут сокета
     */
    public int getSocketTimeout() {
        return getIntProperty("http.socket.timeout", DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * Получить платежный счет для переводов
     * @return Номер платежного счета/карты
     */
    public String getPaymentAccount() {
        return getProperty("payment.account", "5536 9137 9652 9845");
    }

    /**
     * Получить таймаут для HTTP соединений (в секундах)
     * @return Значение таймаута
     */
    public int getHttpConnectTimeout() {
        return Integer.parseInt(getProperty("http.connect.timeout", "10"));
    }

    /**
     * Получить таймаут ожидания ответа HTTP (в секундах)
     * @return Значение таймаута
     */
    public int getHttpReadTimeout() {
        return Integer.parseInt(getProperty("http.read.timeout", "30"));
    }

    /**
     * Получить время кэширования данных токенов (в минутах)
     * @return Время в минутах
     */
    public int getTokenCacheExpiryMinutes() {
        return Integer.parseInt(getProperty("token.cache.expiry.minutes", "30"));
    }
} 