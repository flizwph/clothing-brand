package ru.escapismart.service;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.escapismart.config.AppConfig;

import java.io.IOException;

/**
 * Сервис для выполнения HTTP-запросов с механизмом повторных попыток
 */
public class HttpClientService {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
    private static HttpClientService instance;
    private final CloseableHttpClient httpClient;
    private final AppConfig appConfig;
    private final int maxRetries;
    private final long retryBaseDelay;
    private final long retryMaxDelay;

    /**
     * Приватный конструктор для реализации Singleton
     */
    private HttpClientService() {
        this.appConfig = AppConfig.getInstance();
        this.maxRetries = appConfig.getHttpMaxRetries();
        this.retryBaseDelay = appConfig.getHttpRetryBaseDelay();
        this.retryMaxDelay = appConfig.getHttpRetryMaxDelay();

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(appConfig.getHttpConnectionTimeout())
                .setSocketTimeout(appConfig.getHttpSocketTimeout())
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
        
        logger.info("Инициализирован HttpClientService с таймаутами соединения: {} мс, сокета: {} мс, макс. повторов: {}", 
                appConfig.getHttpConnectionTimeout(), appConfig.getHttpSocketTimeout(), maxRetries);
    }

    /**
     * Получить экземпляр HttpClientService (Singleton)
     * @return экземпляр HttpClientService
     */
    public static synchronized HttpClientService getInstance() {
        if (instance == null) {
            instance = new HttpClientService();
        }
        return instance;
    }

    /**
     * Выполнить GET-запрос с механизмом повторных попыток
     * @param url URL для запроса
     * @return строка с ответом
     * @throws IOException при ошибке выполнения запроса после всех повторов
     */
    public String executeGetWithRetry(String url) throws IOException {
        int attempts = 0;
        IOException lastException = null;

        while (attempts < maxRetries) {
            try {
                HttpGet request = new HttpGet(url);
                logger.debug("Выполняется GET-запрос: {} (попытка {})", url, attempts + 1);
                
                HttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    HttpEntity entity = response.getEntity();
                    String result = EntityUtils.toString(entity);
                    logger.debug("Успешный ответ: {} символов", result.length());
                    return result;
                } else if (statusCode >= 500) {
                    // Серверная ошибка, повторяем
                    logger.warn("Получен код ошибки {}, повторный запрос", statusCode);
                } else {
                    // Клиентская ошибка, не повторяем
                    String errorBody = EntityUtils.toString(response.getEntity());
                    logger.error("Получен код ошибки {}, без повторного запроса. Ответ: {}", statusCode, errorBody);
                    throw new IOException("HTTP ошибка " + statusCode + ": " + errorBody);
                }
            } catch (IOException e) {
                lastException = e;
                logger.warn("Ошибка при выполнении запроса: {} (попытка {})", e.getMessage(), attempts + 1);
            }
            
            attempts++;
            if (attempts < maxRetries) {
                long delay = calculateRetryDelay(attempts);
                logger.debug("Ожидание {} мс перед следующей попыткой", delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Прерывание во время ожидания между попытками", e);
                }
            }
        }
        
        // Если все попытки не удались
        logger.error("Все {} попыток запроса не удались", maxRetries);
        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Не удалось выполнить запрос после " + maxRetries + " попыток");
        }
    }
    
    /**
     * Рассчитать задержку перед следующей попыткой (экспоненциальное увеличение)
     * @param attempt номер попытки
     * @return задержка в миллисекундах
     */
    private long calculateRetryDelay(int attempt) {
        // Экспоненциальное увеличение задержки (2^attempt * baseDelay)
        long delay = retryBaseDelay * (1L << (attempt - 1));
        // Ограничение максимальной задержки
        return Math.min(delay, retryMaxDelay);
    }
    
    /**
     * Закрыть HTTP-клиент
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
                logger.info("HttpClient закрыт");
            }
        } catch (IOException e) {
            logger.error("Ошибка при закрытии HttpClient: {}", e.getMessage());
        }
    }
} 