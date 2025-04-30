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
public class PublicEndpointResilienceTests {
    
    @LocalServerPort
    private int port;
    
    private TestRestTemplate restTemplate;
    private HttpHeaders headers;
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        baseUrl = "http://localhost:" + port;
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости регистрации пользователей")
    public void testUserRegistrationResilience() throws InterruptedException {
        int numThreads = 200;
        int requestsPerThread = 2;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            
            service.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        Map<String, String> request = new HashMap<>();
                        request.put("username", "load_test_user_" + threadNum + "_" + j);
                        request.put("password", "StrongPassword123!");
                        
                        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                baseUrl + "/api/auth/register", 
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
    @DisplayName("Тест отказоустойчивости Discord верификации")
    public void testDiscordVerificationResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Для Discord API требуется API ключ
        headers.set("X-API-KEY", "4f3e2d1c-b5a6-48c7-9d8e-f7g6h5j4k3l2"); // Берем из конфигурации
        
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            service.submit(() -> {
                try {
                    Map<String, String> request = new HashMap<>();
                    request.put("code", "test_verification_code_" + threadNum);
                    request.put("discordUsername", "test_discord_user_" + threadNum);
                    request.put("discordId", String.valueOf(1000000 + threadNum));
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            baseUrl + "/api/discord/verify", 
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
    @DisplayName("Тест отказоустойчивости получения статуса Discord верификации")
    public void testDiscordStatusCheckResilience() throws InterruptedException {
        int numThreads = 300;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            service.submit(() -> {
                try {
                    Map<String, String> request = new HashMap<>();
                    request.put("discordId", String.valueOf(1000000 + threadNum));
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            baseUrl + "/api/discord/check-status", 
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
        System.out.println("Успешные запросы проверки статуса Discord: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения публичных продуктов")
    public void testGetPublicProductsResilience() throws InterruptedException {
        int numThreads = 400;
        ExecutorService service = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/products", 
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
        System.out.println("Успешные запросы публичных продуктов: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения информации о продукте")
    public void testGetProductDetailsResilience() throws InterruptedException {
        int numThreads = 350;
        ExecutorService service = Executors.newFixedThreadPool(45);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int productId = (i % 10) + 1; // Чередуем 10 разных ID продуктов
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/products/" + productId, 
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
        
        latch.await(5, TimeUnit.MINUTES);
        System.out.println("Успешные запросы деталей продукта: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения продуктов по размеру")
    public void testGetProductsBySizeResilience() throws InterruptedException {
        int numThreads = 300;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String[] sizes = {"S", "M", "L", "XL", "XXL"};
        
        for (int i = 0; i < numThreads; i++) {
            final String size = sizes[i % sizes.length]; // Чередуем разные размеры
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/products/size/" + size, 
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
        System.out.println("Успешные запросы продуктов по размеру: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости проверки подписки")
    public void testCheckSubscriptionResilience() throws InterruptedException {
        int numThreads = 250;
        ExecutorService service = Executors.newFixedThreadPool(35);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String[] levels = {"BASIC", "PREMIUM", "VIP"};
        
        for (int i = 0; i < numThreads; i++) {
            final int userId = (i % 20) + 1; // Чередуем 20 разных ID пользователей
            final String level = levels[i % levels.length]; // Чередуем разные уровни подписки
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/subscriptions/check/" + userId + "/" + level, 
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
    @DisplayName("Тест отказоустойчивости проверки настольной подписки")
    public void testCheckDesktopSubscriptionResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final String activationCode = "DESK-" + (10000 + i); // Генерируем уникальные коды активации
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/desktop-subscriptions/check/" + activationCode, 
                            HttpMethod.GET,
                            entity,
                            Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        // Считаем успешными также ответы NOT_FOUND, т.к. это нормальное поведение для несуществующих кодов
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(3, TimeUnit.MINUTES);
        System.out.println("Успешные проверки настольных подписок: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест смешанной нагрузки на публичные эндпоинты")
    public void testMixedPublicEndpointLoad() throws InterruptedException {
        int numThreads = 500;
        ExecutorService service = Executors.newFixedThreadPool(60);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            
            service.submit(() -> {
                try {
                    // Выбираем случайный эндпоинт в зависимости от ID потока
                    int endpointType = threadId % 5;
                    
                    switch (endpointType) {
                        case 0: // Регистрация
                            Map<String, String> regRequest = new HashMap<>();
                            regRequest.put("username", "mixed_test_user_" + threadId);
                            regRequest.put("password", "TestPassword123!");
                            
                            HttpEntity<Map<String, String>> regEntity = new HttpEntity<>(regRequest, headers);
                            ResponseEntity<Map> regResponse = restTemplate.postForEntity(
                                    baseUrl + "/api/auth/register", 
                                    regEntity, 
                                    Map.class);
                            
                            if (regResponse.getStatusCode().is2xxSuccessful() || 
                                    regResponse.getStatusCode() == HttpStatus.CONFLICT) {
                                successCount.incrementAndGet();
                            }
                            break;
                            
                        case 1: // Проверка статуса Discord
                            Map<String, String> discordRequest = new HashMap<>();
                            discordRequest.put("discordId", String.valueOf(1000000 + threadId));
                            
                            HttpEntity<Map<String, String>> discordEntity = new HttpEntity<>(discordRequest, headers);
                            ResponseEntity<Map> discordResponse = restTemplate.postForEntity(
                                    baseUrl + "/api/discord/check-status", 
                                    discordEntity, 
                                    Map.class);
                            
                            if (discordResponse.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            }
                            break;
                            
                        case 2: // Получение списка продуктов
                            HttpEntity<Void> productsEntity = new HttpEntity<>(headers);
                            ResponseEntity<List> productsResponse = restTemplate.exchange(
                                    baseUrl + "/api/products", 
                                    HttpMethod.GET,
                                    productsEntity,
                                    List.class);
                            
                            if (productsResponse.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            }
                            break;
                            
                        case 3: // Получение деталей продукта
                            int productId = (threadId % 10) + 1;
                            HttpEntity<Void> productEntity = new HttpEntity<>(headers);
                            ResponseEntity<Map> productResponse = restTemplate.exchange(
                                    baseUrl + "/api/products/" + productId, 
                                    HttpMethod.GET,
                                    productEntity,
                                    Map.class);
                            
                            if (productResponse.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            }
                            break;
                            
                        case 4: // Проверка подписки
                            String[] levels = {"BASIC", "PREMIUM", "VIP"};
                            int userId = (threadId % 20) + 1;
                            String level = levels[threadId % 3];
                            
                            HttpEntity<Void> subEntity = new HttpEntity<>(headers);
                            ResponseEntity<Map> subResponse = restTemplate.exchange(
                                    baseUrl + "/api/subscriptions/check/" + userId + "/" + level, 
                                    HttpMethod.GET,
                                    subEntity,
                                    Map.class);
                            
                            if (subResponse.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            }
                            break;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(6, TimeUnit.MINUTES);
        System.out.println("Успешные смешанные запросы: " + successCount.get() + " из " + numThreads);
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
        restTemplate.postForEntity(baseUrl + "/api/auth/register", createEntity, Map.class);
        
        for (int i = 0; i < numThreads; i++) {
            service.submit(() -> {
                try {
                    Map<String, String> loginRequest = new HashMap<>();
                    loginRequest.put("username", "concurrent_test_user");
                    loginRequest.put("password", "StrongPassword123!");
                    
                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            baseUrl + "/api/auth/login", 
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
    @DisplayName("Тест отказоустойчивости выхода из системы")
    public void testLogoutResilience() throws InterruptedException {
        int numThreads = 150;
        ExecutorService service = Executors.newFixedThreadPool(25);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Создаем список токенов для выхода из системы
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            // Авторизуемся для получения токена
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "testuser");
            loginRequest.put("password", "testpassword");
            
            HttpEntity<Map<String, String>> loginEntity = new HttpEntity<>(loginRequest, headers);
            ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                    baseUrl + "/api/auth/login", 
                    loginEntity, 
                    Map.class);
            
            if (loginResponse.getStatusCode().is2xxSuccessful()) {
                String token = (String) loginResponse.getBody().get("accessToken");
                tokens.add(token);
            }
        }
        
        // Выполняем параллельный выход из системы
        for (int i = 0; i < tokens.size(); i++) {
            final String token = tokens.get(i);
            service.submit(() -> {
                try {
                    HttpHeaders authHeaders = new HttpHeaders();
                    authHeaders.setContentType(MediaType.APPLICATION_JSON);
                    authHeaders.setBearerAuth(token);
                    
                    HttpEntity<Void> entity = new HttpEntity<>(authHeaders);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/auth/logout", 
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
        System.out.println("Успешные выходы из системы: " + successCount.get() + " из " + tokens.size());
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости сортировки продуктов по цене")
    public void testSortProductsByPriceResilience() throws InterruptedException {
        int numThreads = 250;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String[] sortDirections = {"asc", "desc"};
        
        for (int i = 0; i < numThreads; i++) {
            final String sortDirection = sortDirections[i % sortDirections.length];
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/products/sort/price?direction=" + sortDirection, 
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
        
        latch.await(4, TimeUnit.MINUTES);
        System.out.println("Успешные запросы сортировки продуктов: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения продуктов по категории")
    public void testGetProductsByCategoryResilience() throws InterruptedException {
        int numThreads = 280;
        ExecutorService service = Executors.newFixedThreadPool(40);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String[] categories = {"SHIRT", "HOODIE", "HAT", "ACCESSORY", "PANTS"};
        
        for (int i = 0; i < numThreads; i++) {
            final String category = categories[i % categories.length];
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/products/category/" + category, 
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
        
        latch.await(4, TimeUnit.MINUTES);
        System.out.println("Успешные запросы продуктов по категории: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости поиска продуктов")
    public void testSearchProductsResilience() throws InterruptedException {
        int numThreads = 300;
        ExecutorService service = Executors.newFixedThreadPool(45);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String[] searchTerms = {"shirt", "black", "limited", "new", "special", "hoodie", "brand"};
        
        for (int i = 0; i < numThreads; i++) {
            final String term = searchTerms[i % searchTerms.length];
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<List> response = restTemplate.exchange(
                            baseUrl + "/api/products/search?query=" + term, 
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
        
        latch.await(4, TimeUnit.MINUTES);
        System.out.println("Успешные запросы поиска продуктов: " + successCount.get() + " из " + numThreads);
    }
    
    @Test
    @DisplayName("Тест отказоустойчивости получения статистики доставки")
    public void testGetDeliveryStatsResilience() throws InterruptedException {
        int numThreads = 200;
        ExecutorService service = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        String[] countries = {"Russia", "USA", "Germany", "France", "UK", "Japan", "Australia"};
        
        for (int i = 0; i < numThreads; i++) {
            final String country = countries[i % countries.length];
            
            service.submit(() -> {
                try {
                    HttpEntity<Void> entity = new HttpEntity<>(headers);
                    ResponseEntity<Map> response = restTemplate.exchange(
                            baseUrl + "/api/delivery/stats/" + country, 
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
        System.out.println("Успешные запросы статистики доставки: " + successCount.get() + " из " + numThreads);
    }
} 