package ru.escapismart.services;

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
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для управления HTTP-запросами с конфигурируемыми настройками 
 * таймаутов и механизмом повторных попыток.
 */
public class HttpClientService {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
    
    private final int maxRetries;
    private final int connectionTimeout;
    private final int socketTimeout;
    
    private static HttpClientService instance;
    
    /**
     * Создает экземпляр сервиса с настройками из AppConfig
     */
    private HttpClientService() {
        AppConfig config = AppConfig.getInstance();
        this.maxRetries = config.getHttpMaxRetries();
        this.connectionTimeout = config.getConnectionTimeout();
        this.socketTimeout = config.getSocketTimeout();
        logger.info("HttpClientService инициализирован: maxRetries={}, connectionTimeout={}ms, socketTimeout={}ms", 
                    maxRetries, connectionTimeout, socketTimeout);
    }
    
    /**
     * Получить экземпляр сервиса (Singleton)
     */
    public static synchronized HttpClientService getInstance() {
        if (instance == null) {
            instance = new HttpClientService();
        }
        return instance;
    }
    
    /**
     * Выполняет GET-запрос с поддержкой повторных попыток
     * 
     * @param url URL для запроса
     * @return строка с ответом сервера
     * @throws IOException если все попытки выполнить запрос завершились неудачей
     */
    public String executeGet(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }
        
        logger.debug("Выполнение GET-запроса к URL: {}", url);
        
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= maxRetries) {
            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpGet request = new HttpGet(url);
                HttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    HttpEntity entity = response.getEntity();
                    String responseBody = EntityUtils.toString(entity);
                    logger.debug("Успешный ответ (статус {}): {}", statusCode, 
                               responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody);
                    return responseBody;
                } else if (statusCode >= 500) {
                    // Серверная ошибка, повторяем запрос
                    logger.warn("Получен серверный статус ошибки: {}. Попытка {}/{}.", 
                              statusCode, retryCount + 1, maxRetries + 1);
                    lastException = new IOException("Серверная ошибка с кодом: " + statusCode);
                } else {
                    // Клиентская ошибка, не повторяем запрос
                    String errorBody = EntityUtils.toString(response.getEntity());
                    logger.error("Получен клиентский статус ошибки: {}. Тело ответа: {}", 
                               statusCode, errorBody);
                    throw new IOException("Клиентская ошибка с кодом: " + statusCode);
                }
            } catch (SocketTimeoutException e) {
                logger.warn("Таймаут сокета при выполнении запроса. Попытка {}/{}.", 
                          retryCount + 1, maxRetries + 1);
                lastException = e;
            } catch (IOException e) {
                logger.warn("Ошибка ввода-вывода при выполнении запроса: {}. Попытка {}/{}.", 
                          e.getMessage(), retryCount + 1, maxRetries + 1);
                lastException = e;
            }
            
            if (retryCount < maxRetries) {
                long sleepTime = calculateBackoffTime(retryCount);
                logger.debug("Ожидание {} мс перед следующей попыткой", sleepTime);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Прерывание потока во время ожидания следующей попытки", e);
                }
            }
            
            retryCount++;
        }
        
        String errorMessage = "Исчерпаны все попытки выполнения запроса к " + url;
        logger.error(errorMessage, lastException);
        
        if (lastException != null) {
            if (lastException instanceof IOException) {
                throw (IOException) lastException;
            } else {
                throw new IOException(errorMessage, lastException);
            }
        } else {
            throw new IOException(errorMessage);
        }
    }
    
    /**
     * Вычисляет время ожидания с экспоненциальным увеличением
     *
     * @param retryCount текущий номер попытки
     * @return время ожидания в миллисекундах
     */
    private long calculateBackoffTime(int retryCount) {
        // Начальное время ожидания 1000 мс (1 секунда)
        long initialTimeout = 1000;
        // Максимальное время ожидания 30000 мс (30 секунд)
        long maxTimeout = 30000;
        
        long backoffTime = initialTimeout * (long) Math.pow(2, retryCount);
        return Math.min(backoffTime, maxTimeout);
    }
    
    /**
     * Создает HTTP-клиент с настроенными таймаутами
     *
     * @return настроенный HTTP-клиент
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .build();
                
        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }
} 