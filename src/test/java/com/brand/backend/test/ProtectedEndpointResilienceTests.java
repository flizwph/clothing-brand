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

@SpringBootTest(classes = ClothingBrandApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.CONCURRENT)
@Tag("resilience")
public class ProtectedEndpointResilienceTests {
    
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
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "testpassword");
        
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
    @DisplayName("Тест отказоустойчивости обновления токена")
    public void testTokenRefreshResilience() throws InterruptedException {
        int numThreads = 150;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Получение refresh токена
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "testpassword");
        
        HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", 
                loginEntity, 
                Map.class);
        
        String refreshToken = null;
        if (loginResponse.getStatusCode().is2xxSuccessful()) {
            refreshToken = (String) loginResponse.getBody().get("refreshToken");
        }
        
        if (refreshToken != null) {
            final String finalRefreshToken = refreshToken;
            
            for (int i = 0; i < numThreads; i++) {
                service.submit(() -> {
                    try {
                        Map<String, String> refreshRequest = new HashMap<>();
                        refreshRequest.put("refreshToken", finalRefreshToken);
                        
                        HttpEntity<Map<String, String>> entity = new HttpEntity<>(refreshRequest, headers);
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                baseUrl + "/api/auth/refresh", 
                                entity, 
                                Map.class);
                        
                        if (response.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(2, TimeUnit.MINUTES);
            System.out.println("Успешные обновления токена: " + successCount.get() + " из " + numThreads);
        }
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения информации о пользователе")
    public void testGetUserInfoResilience() throws InterruptedException {
        int numThreads = 300;
        ExecutorService service = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/users/me", 
                            HttpMethod.GET,
                            entity,
                            Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные запросы информации о пользователе: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости проверки статуса верификации")
    public void testCheckVerificationStatusResilience() throws InterruptedException {
        int numThreads = 250;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/users/is-verified", 
                            HttpMethod.GET,
                            entity,
                            Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные проверки статуса верификации: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости обновления профиля пользователя")
    public void testUpdateUserProfileResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    Map<String, String> updateRequest = new HashMap<>();
                    updateRequest.put("newUsername", "updated_user_" + threadId);
                    updateRequest.put("email", "user" + threadId + "@example.com");
                    updateRequest.put("phoneNumber", "+7900" + String.format("%07d", threadId));
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(updateRequest, headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/users/update", 
                            HttpMethod.PUT,
                            entity,
                            Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные обновления профиля: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения NFT пользователя")
    public void testGetUserNFTsResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/nfts/me", 
                            HttpMethod.GET,
                            entity,
                            List.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные запросы NFT пользователя: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости создания заказа")
    public void testCreateOrderResilience() throws InterruptedException {
        int numThreads = 150;
        ExecutorService service = Executors.newFixedThreadPool(25);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    Map<String, Object> orderRequest = new HashMap<>();
                    orderRequest.put("productId", 1L);
                    orderRequest.put("quantity", 1);
                    orderRequest.put("size", "M");
                    orderRequest.put("email", "order" + threadId + "@example.com");
                    orderRequest.put("phoneNumber", "+7911" + String.format("%07d", threadId));
                    orderRequest.put("fullName", "Test User " + threadId);
                    orderRequest.put("country", "Russia");
                    orderRequest.put("address", "Test Address " + threadId);
                    orderRequest.put("postalCode", "123456");
                    orderRequest.put("paymentMethod", "card");
                    
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            baseUrl + "/api/orders", 
                            entity, 
                            Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.MINUTES);
        System.out.println("Успешные создания заказов: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения заказов пользователя")
    public void testGetUserOrdersResilience() throws InterruptedException {
        int numThreads = 180;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/orders", 
                            HttpMethod.GET,
                            entity,
                            List.class);
                    
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные запросы заказов пользователя: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости активации подписки")
    public void testActivateSubscriptionResilience() throws InterruptedException {
        int numThreads = 120;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final String activationCode = "SUB-" + String.format("%06d", i);
            service.submit(() -> {
                try {
                    Map<String, String> activateRequest = new HashMap<>();
                    activateRequest.put("activationCode", activationCode);
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(activateRequest, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            baseUrl + "/api/subscriptions/activate", 
                            entity, 
                            Map.class);
                    
                    // 404 тоже считаем успехом, так как это ожидаемое поведение для несуществующих кодов
                    if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные активации подписок: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости смешанных защищенных запросов")
    public void testMixedProtectedRequests() throws InterruptedException {
        int numThreads = 300;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Выбираем тип запроса в зависимости от ID потока
                    int requestType = threadId % 5;
                    ResponseEntity<?> response = null;
                    
                    switch (requestType) {
                        case 0: // Получение данных пользователя
                            HttpEntity<Void> userEntity = new HttpEntity<>(headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/users/me", 
                                    HttpMethod.GET,
                                    userEntity,
                                    Map.class);
                            break;
                            
                        case 1: // Получение NFT
                            HttpEntity<Void> nftEntity = new HttpEntity<>(headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/nfts/me", 
                                    HttpMethod.GET,
                                    nftEntity,
                                    List.class);
                            break;
                            
                        case 2: // Получение заказов
                            HttpEntity<Void> ordersEntity = new HttpEntity<>(headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/orders", 
                                    HttpMethod.GET,
                                    ordersEntity,
                                    List.class);
                            break;
                            
                        case 3: // Обновление профиля
                            Map<String, String> updateRequest = new HashMap<>();
                            updateRequest.put("email", "mixed" + threadId + "@example.com");
                            
                            HttpEntity<Map<String, String>> updateEntity = new HttpEntity<>(updateRequest, headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/users/update", 
                                    HttpMethod.PUT,
                                    updateEntity,
                                    Map.class);
                            break;
                            
                        case 4: // Проверка статуса верификации
                            HttpEntity<Void> verifyEntity = new HttpEntity<>(headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/users/is-verified", 
                                    HttpMethod.GET,
                                    verifyEntity,
                                    Map.class);
                            break;
                    }
                    
                    if (response != null && response.getStatusCode().is2xxSuccessful()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.MINUTES);
        System.out.println("Успешные смешанные защищенные запросы: " + successCount.get() + " из " + numThreads);
    }
} 