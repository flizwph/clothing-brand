package com.brand.backend.test;

import com.brand.backend.applicationstart.ClothingBrandApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Тесты экстремально высокой нагрузки на систему
 * для проверки производительности и отказоустойчивости
 */
@SpringBootTest(classes = ClothingBrandApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.CONCURRENT)
@Tag("highload")
@Tag("resilience")
public class HighLoadResilienceTests {
    
    @LocalServerPort
    private int port;
    
    private TestRestTemplate restTemplate;
    private HttpHeaders headers;
    private String baseUrl;
    private String accessToken;
    
    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        baseUrl = "http://localhost:" + port;
        
        // Аутентификация и получение токена
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "ваня123456");
        loginRequest.put("password", "123456");
        
        HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", 
                loginEntity, 
                Map.class);
        
        if (loginResponse.getStatusCode().is2xxSuccessful()) {
            accessToken = (String) loginResponse.getBody().get("accessToken");
            headers.setBearerAuth(accessToken);
        }
    }
    
    @Test
    @DisplayName("Тест предельной нагрузки на регистрацию и авторизацию")
    public void testHighLoadAuthEndpoints() throws InterruptedException {
        int numThreads = 1000;
        ExecutorService service = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Чередуем запросы регистрации и авторизации
                    if (threadId % 2 == 0) {
                        // Регистрация
                        Map<String, String> request = new HashMap<>();
                        request.put("username", "ваня123456" + threadId);
                        request.put("password", "123456");
                        
                        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                        
                        try {
                            ResponseEntity<Map> response = restTemplate.postForEntity(
                                    baseUrl + "/api/auth/register", 
                                    entity, 
                                    Map.class);
                            
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            timeoutCount.incrementAndGet();
                        }
                    } else {
                        // Авторизация
                        Map<String, String> request = new HashMap<>();
                        request.put("username", "ваня123456");
                        request.put("password", "123456");
                        
                        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                        try {
                            ResponseEntity<Map> response = restTemplate.postForEntity(
                                    baseUrl + "/api/auth/login", 
                                    entity, 
                                    Map.class);
                            
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            timeoutCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.MINUTES);
        System.out.println("Тест предельной нагрузки на аутентификацию:");
        System.out.println("Успешные запросы: " + successCount.get() + " из " + numThreads);
        System.out.println("Неуспешные запросы: " + failureCount.get());
        System.out.println("Таймауты: " + timeoutCount.get());
    }
    
    @Test
    @DisplayName("Тест массированной нагрузки на получение продуктов")
    public void testHighLoadProductEndpoints() throws InterruptedException {
        int numThreads = 2000;
        ExecutorService service = Executors.newFixedThreadPool(150);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        
        String[] categories = {"SHIRT", "HOODIE", "HAT", "ACCESSORY", "PANTS"};
        String[] sortDirections = {"asc", "desc"};
        String[] sizes = {"S", "M", "L", "XL", "XXL"};
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<?> response = null;
                    
                    try {
                        // Выбираем тип запроса в зависимости от ID потока
                        int requestType = threadId % 5;
                        
                        switch (requestType) {
                            case 0: // Получение всех продуктов
                                response = restTemplate.exchange(
                                        baseUrl + "/api/products", 
                                        HttpMethod.GET,
                                        entity,
                                        List.class);
                                break;
                                
                            case 1: // Получение продуктов по категории
                                String category = categories[threadId % categories.length];
                                response = restTemplate.exchange(
                                        baseUrl + "/api/products/category/" + category, 
                                        HttpMethod.GET,
                                        entity,
                                        List.class);
                                break;
                                
                            case 2: // Сортировка продуктов по цене
                                String direction = sortDirections[threadId % sortDirections.length];
                                response = restTemplate.exchange(
                                        baseUrl + "/api/products/sort/price?direction=" + direction, 
                                        HttpMethod.GET,
                                        entity,
                                        List.class);
                                break;
                                
                            case 3: // Получение продуктов по размеру
                                String size = sizes[threadId % sizes.length];
                                response = restTemplate.exchange(
                                        baseUrl + "/api/products/size/" + size, 
                                        HttpMethod.GET,
                                        entity,
                                        List.class);
                                break;
                                
                            case 4: // Получение деталей продукта
                                long productId = (threadId % 10) + 1; // Предполагается, что есть 10 продуктов
                                response = restTemplate.exchange(
                                        baseUrl + "/api/products/" + productId, 
                                        HttpMethod.GET,
                                        entity,
                                        Map.class);
                                break;
                        }
                        
                        if (response != null && response.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        timeoutCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.MINUTES);
        System.out.println("Тест предельной нагрузки на продуктовые эндпоинты:");
        System.out.println("Успешные запросы: " + successCount.get() + " из " + numThreads);
        System.out.println("Неуспешные запросы: " + failureCount.get());
        System.out.println("Таймауты: " + timeoutCount.get());
    }
    
    @Test
    @DisplayName("Тест экстремальной смешанной нагрузки на все эндпоинты")
    public void testExtremeLoadAllEndpoints() throws InterruptedException {
        int numThreads = 5000;
        ExecutorService service = Executors.newFixedThreadPool(300);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Распределяем запросы по всем доступным эндпоинтам
                    int endpointType = threadId % 10;
                    
                    try {
                        switch (endpointType) {
                            case 0: // Регистрация
                                Map<String, String> regRequest = new HashMap<>();
                                regRequest.put("username", "extreme_user_" + threadId);
                                regRequest.put("password", "StrongPassword123!");
                                
                                HttpEntity<Map<String, String>> regEntity = new HttpEntity<>(regRequest, headers);
                                ResponseEntity<Map> regResponse = restTemplate.postForEntity(
                                        baseUrl + "/api/auth/register", 
                                        regEntity, 
                                        Map.class);
                                
                                if (regResponse.getStatusCode().is2xxSuccessful() || 
                                        regResponse.getStatusCode() == HttpStatus.CONFLICT) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 1: // Получение всех продуктов
                                HttpEntity<Void> prodEntity = new HttpEntity<>(headers);
                                ResponseEntity<List> prodResponse = restTemplate.exchange(
                                        baseUrl + "/api/products", 
                                        HttpMethod.GET,
                                        prodEntity,
                                        List.class);
                                
                                if (prodResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 2: // Discord верификация
                                Map<String, String> discordRequest = new HashMap<>();
                                discordRequest.put("code", "extreme_test_code_" + threadId);
                                discordRequest.put("discordUsername", "extreme_discord_user_" + threadId);
                                discordRequest.put("discordId", String.valueOf(2000000 + threadId));
                                
                                HttpEntity<Map<String, String>> discordEntity = new HttpEntity<>(discordRequest, headers);
                                ResponseEntity<Map> discordResponse = restTemplate.postForEntity(
                                        baseUrl + "/api/discord/verify", 
                                        discordEntity, 
                                        Map.class);
                                
                                if (discordResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 3: // Авторизация
                                Map<String, String> loginRequest = new HashMap<>();
                                loginRequest.put("username", "testuser");
                                loginRequest.put("password", "testpassword");
                                
                                HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);
                                ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                                        baseUrl + "/api/auth/login", 
                                        loginEntity, 
                                        Map.class);
                                
                                if (loginResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 4: // Получение информации о пользователе
                                HttpHeaders authHeaders = new HttpHeaders();
                                authHeaders.setContentType(MediaType.APPLICATION_JSON);
                                authHeaders.setBearerAuth(accessToken);
                                
                                HttpEntity<Void> userEntity = new HttpEntity<>(authHeaders);
                                ResponseEntity<Map> userResponse = restTemplate.exchange(
                                        baseUrl + "/api/users/me", 
                                        HttpMethod.GET,
                                        userEntity,
                                        Map.class);
                                
                                if (userResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 5: // Проверка подписки
                                long userId = (threadId % 20) + 1;
                                String level = threadId % 2 == 0 ? "BASIC" : "PREMIUM";
                                
                                HttpEntity<Void> subEntity = new HttpEntity<>(headers);
                                ResponseEntity<Map> subResponse = restTemplate.exchange(
                                        baseUrl + "/api/subscriptions/check/" + userId + "/" + level, 
                                        HttpMethod.GET,
                                        subEntity,
                                        Map.class);
                                
                                if (subResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 6: // Получение заказов пользователя
                                HttpHeaders orderHeaders = new HttpHeaders();
                                orderHeaders.setContentType(MediaType.APPLICATION_JSON);
                                orderHeaders.setBearerAuth(accessToken);
                                
                                HttpEntity<Void> orderEntity = new HttpEntity<>(orderHeaders);
                                ResponseEntity<List> orderResponse = restTemplate.exchange(
                                        baseUrl + "/api/orders", 
                                        HttpMethod.GET,
                                        orderEntity,
                                        List.class);
                                
                                if (orderResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 7: // Поиск продуктов
                                String[] searchTerms = {"shirt", "black", "limited", "brand"};
                                String term = searchTerms[threadId % searchTerms.length];
                                
                                HttpEntity<Void> searchEntity = new HttpEntity<>(headers);
                                ResponseEntity<List> searchResponse = restTemplate.exchange(
                                        baseUrl + "/api/products/search?query=" + term, 
                                        HttpMethod.GET,
                                        searchEntity,
                                        List.class);
                                
                                if (searchResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 8: // Получение NFT пользователя
                                HttpHeaders nftHeaders = new HttpHeaders();
                                nftHeaders.setContentType(MediaType.APPLICATION_JSON);
                                nftHeaders.setBearerAuth(accessToken);
                                
                                HttpEntity<Void> nftEntity = new HttpEntity<>(nftHeaders);
                                ResponseEntity<List> nftResponse = restTemplate.exchange(
                                        baseUrl + "/api/nfts/me", 
                                        HttpMethod.GET,
                                        nftEntity,
                                        List.class);
                                
                                if (nftResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                                
                            case 9: // Проверка статуса Discord
                                Map<String, String> statusRequest = new HashMap<>();
                                statusRequest.put("discordId", String.valueOf(2000000 + threadId));
                                
                                HttpEntity<Map<String, String>> statusEntity = new HttpEntity<>(statusRequest, headers);
                                ResponseEntity<Map> statusResponse = restTemplate.postForEntity(
                                        baseUrl + "/api/discord/check-status", 
                                        statusEntity, 
                                        Map.class);
                                
                                if (statusResponse.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                                break;
                        }
                    } catch (Exception e) {
                        timeoutCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(15, TimeUnit.MINUTES);
        System.out.println("Тест экстремальной нагрузки на все эндпоинты:");
        System.out.println("Успешные запросы: " + successCount.get() + " из " + numThreads);
        System.out.println("Неуспешные запросы: " + failureCount.get());
        System.out.println("Таймауты: " + timeoutCount.get());
        
        // Проверяем, что система выдержала нагрузку
        double successRate = (double) successCount.get() / numThreads;
        System.out.println("Процент успешных запросов: " + (successRate * 100) + "%");
        
        // Минимально приемлемый процент успешных запросов при экстремальной нагрузке
        // Можно настроить в зависимости от требований к системе
        double minAcceptableSuccessRate = 0.5; // 50%
        org.junit.jupiter.api.Assertions.assertTrue(
                successRate >= minAcceptableSuccessRate,
                "Процент успешных запросов (" + (successRate * 100) + "%) ниже минимально допустимого (" +
                        (minAcceptableSuccessRate * 100) + "%)");
    }
} 