package ru.escapismart.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP-клиент с поддержкой повторных попыток при неудачных запросах.
 */
public class RetryableHttpClient {
    private static final Logger log = LoggerFactory.getLogger(RetryableHttpClient.class);
    
    private final int maxRetries;
    private final int initialTimeout;
    private final int maxTimeout;
    private final int connectTimeout;
    private final int socketTimeout;
    
    /**
     * Создает HTTP-клиент с настройками по умолчанию.
     */
    public RetryableHttpClient() {
        this(3, 1000, 5000, 5000, 5000);
    }
    
    /**
     * Создает HTTP-клиент с указанными настройками.
     *
     * @param maxRetries максимальное число повторных попыток
     * @param initialTimeout начальное время ожидания между попытками (мс)
     * @param maxTimeout максимальное время ожидания между попытками (мс)
     * @param connectTimeout таймаут соединения (мс)
     * @param socketTimeout таймаут сокета (мс)
     */
    public RetryableHttpClient(int maxRetries, int initialTimeout, int maxTimeout, 
                               int connectTimeout, int socketTimeout) {
        this.maxRetries = maxRetries;
        this.initialTimeout = initialTimeout;
        this.maxTimeout = maxTimeout;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
    }
    
    /**
     * Выполняет GET-запрос с поддержкой повторных попыток.
     *
     * @param url URL для запроса
     * @return строка с ответом сервера
     * @throws IOException если все попытки выполнить запрос завершились неудачей
     */
    public String executeGet(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }
        
        log.debug("Выполнение GET-запроса к URL: {}", url);
        
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
                    log.debug("Успешный ответ (статус {}): {}", statusCode, 
                              responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody);
                    return responseBody;
                } else if (statusCode >= 500) {
                    // Серверная ошибка, повторяем запрос
                    log.warn("Получен серверный статус ошибки: {}. Попытка {}/{}.", 
                             statusCode, retryCount + 1, maxRetries + 1);
                    lastException = new IOException("Серверная ошибка с кодом: " + statusCode);
                } else {
                    // Клиентская ошибка, не повторяем запрос
                    String errorBody = EntityUtils.toString(response.getEntity());
                    log.error("Получен клиентский статус ошибки: {}. Тело ответа: {}", 
                              statusCode, errorBody);
                    throw new IOException("Клиентская ошибка с кодом: " + statusCode);
                }
            } catch (SocketTimeoutException e) {
                log.warn("Таймаут сокета при выполнении запроса. Попытка {}/{}.", 
                         retryCount + 1, maxRetries + 1);
                lastException = e;
            } catch (IOException e) {
                log.warn("Ошибка ввода-вывода при выполнении запроса: {}. Попытка {}/{}.", 
                         e.getMessage(), retryCount + 1, maxRetries + 1);
                lastException = e;
            }
            
            if (retryCount < maxRetries) {
                long sleepTime = calculateBackoffTime(retryCount);
                log.debug("Ожидание {} мс перед следующей попыткой", sleepTime);
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
        log.error(errorMessage, lastException);
        
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
     * Выполняет POST-запрос с поддержкой повторных попыток.
     *
     * @param url URL для запроса
     * @param entity данные для отправки
     * @return строка с ответом сервера
     * @throws IOException если все попытки выполнить запрос завершились неудачей
     */
    public String executePost(String url, HttpEntity entity) throws IOException {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }
        
        if (entity == null) {
            throw new IllegalArgumentException("Тело запроса не может быть пустым");
        }
        
        log.debug("Выполнение POST-запроса к URL: {}", url);
        
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount <= maxRetries) {
            try (CloseableHttpClient httpClient = createHttpClient()) {
                HttpPost request = new HttpPost(url);
                request.setEntity(entity);
                
                // Добавляем безопасные заголовки
                request.setHeader("User-Agent", "VK-Escapism-Bot/1.0");
                request.setHeader("Accept", "application/json");
                
                HttpResponse response = httpClient.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                
                if (statusCode >= 200 && statusCode < 300) {
                    HttpEntity responseEntity = response.getEntity();
                    String responseBody = EntityUtils.toString(responseEntity);
                    log.debug("Успешный ответ (статус {}): {}", statusCode, 
                              responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody);
                    return responseBody;
                } else if (statusCode >= 500) {
                    // Серверная ошибка, повторяем запрос
                    log.warn("Получен серверный статус ошибки: {}. Попытка {}/{}.", 
                             statusCode, retryCount + 1, maxRetries + 1);
                    lastException = new IOException("Серверная ошибка с кодом: " + statusCode);
                } else {
                    // Клиентская ошибка, не повторяем запрос
                    String errorBody = EntityUtils.toString(response.getEntity());
                    log.error("Получен клиентский статус ошибки: {}. Тело ответа: {}", 
                              statusCode, errorBody);
                    throw new IOException("Клиентская ошибка с кодом: " + statusCode);
                }
            } catch (SocketTimeoutException e) {
                log.warn("Таймаут сокета при выполнении запроса. Попытка {}/{}.", 
                         retryCount + 1, maxRetries + 1);
                lastException = e;
            } catch (IOException e) {
                log.warn("Ошибка ввода-вывода при выполнении запроса: {}. Попытка {}/{}.", 
                         e.getMessage(), retryCount + 1, maxRetries + 1);
                lastException = e;
            }
            
            if (retryCount < maxRetries) {
                long sleepTime = calculateBackoffTime(retryCount);
                log.debug("Ожидание {} мс перед следующей попыткой", sleepTime);
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Прерывание потока во время ожидания следующей попытки", e);
                }
            }
            
            retryCount++;
        }
        
        String errorMessage = "Исчерпаны все попытки выполнения POST-запроса к " + url;
        log.error(errorMessage, lastException);
        
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
     * Вычисляет время ожидания с экспоненциальным увеличением.
     *
     * @param retryCount текущий номер попытки
     * @return время ожидания в миллисекундах
     */
    private long calculateBackoffTime(int retryCount) {
        long backoffTime = initialTimeout * (long) Math.pow(2, retryCount);
        return Math.min(backoffTime, maxTimeout);
    }
    
    /**
     * Создает HTTP-клиент с настроенными таймаутами.
     *
     * @return настроенный HTTP-клиент
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
                
        return HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }
} 