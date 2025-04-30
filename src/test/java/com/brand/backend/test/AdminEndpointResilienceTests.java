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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(classes = ClothingBrandApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.CONCURRENT)
@Tag("resilience")
@Tag("admin")
public class AdminEndpointResilienceTests {
    
    @LocalServerPort
    private int port;
    
    private TestRestTemplate restTemplate;
    private HttpHeaders headers;
    private String baseUrl;
    private String adminAccessToken;
    
    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        baseUrl = "http://localhost:" + port;
        
        // Аутентификация и получение токена администратора
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "admin");
        loginRequest.put("password", "adminpassword");
        
        HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", 
                loginEntity, 
                Map.class);
        
        if (loginResponse.getStatusCode().is2xxSuccessful()) {
            adminAccessToken = (String) loginResponse.getBody().get("accessToken");
            headers.setBearerAuth(adminAccessToken);
        }
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения всех заказов")
    public void testGetAllOrdersResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/admin/orders", 
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
        System.out.println("Успешные запросы всех заказов: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости обновления статуса заказа")
    public void testUpdateOrderStatusResilience() throws InterruptedException {
        int numThreads = 150;
        ExecutorService service = Executors.newFixedThreadPool(25);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // ID заказов для обновления (предполагается, что в системе есть заказы с ID от 1 до 10)
        long[] orderIds = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
        String[] statuses = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"};
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Выбираем ID заказа и статус в зависимости от номера потока
                    long orderId = orderIds[threadId % orderIds.length];
                    String status = statuses[threadId % statuses.length];
                    
                    Map<String, String> statusRequest = new HashMap<>();
                    statusRequest.put("status", status);
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusRequest, headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/admin/orders/" + orderId + "/status", 
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
        System.out.println("Успешные обновления статуса заказа: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости раскрытия NFT")
    public void testRevealNFTResilience() throws InterruptedException {
        int numThreads = 120;
        ExecutorService service = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // ID NFT для раскрытия (предполагается, что в системе есть NFT с ID от 1 до 10)
        long[] nftIds = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L};
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Выбираем ID NFT в зависимости от номера потока
                    long nftId = nftIds[threadId % nftIds.length];
                    
                    Map<String, String> revealRequest = new HashMap<>();
                    revealRequest.put("revealedUri", "https://example.com/nft/revealed_" + threadId + ".png");
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(revealRequest, headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/admin/nft/" + nftId + "/reveal", 
                            HttpMethod.POST,
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
        System.out.println("Успешные раскрытия NFT: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости смешанных административных запросов")
    public void testMixedAdminRequests() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // ID для обновления
        long[] orderIds = {1L, 2L, 3L, 4L, 5L};
        long[] nftIds = {1L, 2L, 3L, 4L, 5L};
        String[] statuses = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED"};
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Выбираем тип запроса в зависимости от ID потока
                    int requestType = threadId % 3;
                    ResponseEntity<?> response = null;
                    
                    switch (requestType) {
                        case 0: // Получение всех заказов
                            HttpEntity<Void> ordersEntity = new HttpEntity<>(headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/admin/orders", 
                                    HttpMethod.GET,
                                    ordersEntity,
                                    List.class);
                            break;
                            
                        case 1: // Обновление статуса заказа
                            long orderId = orderIds[threadId % orderIds.length];
                            String status = statuses[threadId % statuses.length];
                            
                            Map<String, String> statusRequest = new HashMap<>();
                            statusRequest.put("status", status);
                            
                            HttpEntity<Map<String, String>> statusEntity = new HttpEntity<>(statusRequest, headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/admin/orders/" + orderId + "/status", 
                                    HttpMethod.PUT,
                                    statusEntity,
                                    Map.class);
                            break;
                            
                        case 2: // Раскрытие NFT
                            long nftId = nftIds[threadId % nftIds.length];
                            
                            Map<String, String> revealRequest = new HashMap<>();
                            revealRequest.put("revealedUri", "https://example.com/nft/mixed_" + threadId + ".png");
                            
                            HttpEntity<Map<String, String>> revealEntity = new HttpEntity<>(revealRequest, headers);
                            response = restTemplate.exchange(
                                    baseUrl + "/api/admin/nft/" + nftId + "/reveal", 
                                    HttpMethod.POST,
                                    revealEntity,
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
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные смешанные административные запросы: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости под высокой нагрузкой")
    public void testHighLoadAdminRequests() throws InterruptedException {
        int numThreads = 500;
        ExecutorService service = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            service.submit(() -> {
                try {
                    // Получаем все заказы - наиболее частый запрос администратора
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/admin/orders", 
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
        
        latch.await(5, TimeUnit.MINUTES);
        System.out.println("Успешные запросы под высокой нагрузкой: " + successCount.get() + " из " + numThreads);
    }
} 