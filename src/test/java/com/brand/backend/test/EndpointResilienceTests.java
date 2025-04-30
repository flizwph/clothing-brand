package com.brand.backend.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.CONCURRENT)
@Tag("resilience")
public class EndpointResilienceTests {
    
    private TestRestTemplate restTemplate;
    private HttpHeaders headers;
    private String accessToken;
    
    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Аутентификация и получение токена
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "testpassword");
        
        HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", 
                loginEntity, 
                Map.class);
        
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            accessToken = (String) loginResponse.getBody().get("accessToken");
            headers.setBearerAuth(accessToken);
        }
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости регистрации пользователей")
    public void testUserRegistrationResilience() throws InterruptedException {
        int numThreads = 100;
        int requestsPerThread = 5;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            
            service.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        Map<String, String> request = new HashMap<>();
                        request.put("username", "stress_test_user_" + threadNum + "_" + j);
                        request.put("password", "StrongPassword123!");
                        
                        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                "/api/auth/register", 
                                entity, 
                                Map.class);
                        
                        if (response.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.MINUTES);
        System.out.println("Успешные регистрации: " + successCount.get() + " из " + (numThreads * requestsPerThread));
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости авторизации пользователей")
    public void testUserLoginResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Создание тестового пользователя, если он еще не существует
        Map<String, String> createUser = new HashMap<>();
        createUser.put("username", "concurrent_test_user");
        createUser.put("password", "StrongPassword123!");
        
        HttpEntity<Map<String, String>> createEntity = new HttpEntity<>(createUser, headers);
        restTemplate.postForEntity("/api/auth/register", createEntity, Map.class);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    Map<String, String> loginRequest = new HashMap<>();
                    loginRequest.put("username", "concurrent_test_user");
                    loginRequest.put("password", "StrongPassword123!");
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            "/api/auth/login", 
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
        System.out.println("Успешные авторизации: " + successCount.get() + " из " + numThreads);
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
                "/api/auth/login", 
                loginEntity, 
                Map.class);
        
        String refreshToken = null;
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
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
                                "/api/auth/refresh", 
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
                            "/api/users/me", 
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
    @DisplayName("Тест отказоустойчивости Discord верификации")
    public void testDiscordVerificationResilience() throws InterruptedException {
        int numThreads = 100;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        headers.set("X-API-KEY", "test_api_key"); // Подставьте фактический ключ API
        
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            service.submit(() -> {
                try {
                    Map<String, String> request = new HashMap<>();
                    request.put("code", "test_verification_code");
                    request.put("discordUsername", "test_discord_user_" + threadNum);
                    request.put("discordId", String.valueOf(1000000 + threadNum));
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            "/api/discord/verify", 
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
        System.out.println("Успешные запросы верификации Discord: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения всех продуктов")
    public void testGetAllProductsResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            "/api/products", 
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
        System.out.println("Успешные запросы всех продуктов: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости создания заказа")
    public void testCreateOrderResilience() throws InterruptedException {
        int numThreads = 150;
        ExecutorService service = Executors.newFixedThreadPool(25);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            service.submit(() -> {
                try {
                    Map<String, Object> orderRequest = new HashMap<>();
                    orderRequest.put("totalPrice", 100.0);
                    orderRequest.put("paymentMethod", "CRYPTO");
                    
                    List<Map<String, Object>> items = new ArrayList<>();
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", 1L);
                    item.put("quantity", 1);
                    items.add(item);
                    
                    orderRequest.put("items", items);
                    
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderRequest, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            "/api/orders", 
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
    @DisplayName("Тест отказоустойчивости проверки подписок")
    public void testCheckSubscriptionResilience() throws InterruptedException {
        int numThreads = 250;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            "/api/subscriptions/check/1/BASIC", 
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
        System.out.println("Успешные проверки подписок: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения всех заказов (админ)")
    public void testGetAllOrdersAdminResilience() throws InterruptedException {
        int numThreads = 120;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Получение токена админа (для примера используем тот же метод)
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "admin");
        loginRequest.put("password", "adminpassword");
        
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, adminHeaders);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", 
                loginEntity, 
                Map.class);
        
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            String adminToken = (String) loginResponse.getBody().get("accessToken");
            adminHeaders.setBearerAuth(adminToken);
            
            for (int i = 0; i < numThreads; i++) {
                service.submit(() -> {
                    try {
                        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders);
                        ResponseEntity<List> response = restTemplate.exchange(
                                "/api/admin/orders", 
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
            System.out.println("Успешные запросы всех заказов (админ): " + successCount.get() + " из " + numThreads);
        }
    }
} 