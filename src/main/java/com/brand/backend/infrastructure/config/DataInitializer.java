package com.brand.backend.infrastructure.config;

import com.brand.backend.application.product.service.ProductService;
import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.domain.product.repository.DigitalProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Компонент для инициализации необходимых данных при запуске приложения
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {
    
    private final JdbcTemplate jdbcTemplate;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final DigitalProductRepository digitalProductRepository;
    
    @Value("${app.init-data:true}")
    private boolean shouldInitData;
    
    @PostConstruct
    public void initializeData() {
        if (!shouldInitData) {
            log.info("Инициализация данных отключена");
            return;
        }
        
        log.info("Начинаем инициализацию базовых данных...");
        
        try {
            initializeTables();
            createSampleProducts();
            createSampleDigitalProducts();
            log.info("Инициализация данных завершена успешно");
        } catch (Exception e) {
            log.error("Ошибка при инициализации данных: {}", e.getMessage(), e);
        }
    }
    
    private void initializeTables() {
        log.debug("Проверяем и создаем необходимые таблицы...");
        
        // Здесь можно добавить проверки существования таблиц и их создание при необходимости
        // В данном случае полагаемся на Hibernate и миграции Flyway
    }
    
    private void createSampleProducts() {
        try {
            // Проверяем, есть ли уже товары в базе
            long productCount = productRepository.count();
            if (productCount > 0) {
                log.debug("Товары уже существуют в базе данных ({}), пропускаем создание", productCount);
                return;
            }
            
            log.info("Создаем тестовые товары...");
            
            // Создаем тестовые товары
            Product product1 = Product.createProduct(
                "Gothic T-Shirt Black",
                2500.0,
                "https://example.com/gothic-tshirt.jpg",
                10, 15, 8
            );
            
            Product product2 = Product.createProduct(
                "Dark Angel Tee",
                2800.0,
                "https://example.com/dark-angel-tee.jpg",
                5, 12, 7
            );
            
            Product product3 = Product.createProduct(
                "Oblivium Hoodie",
                4500.0,
                "https://example.com/oblivium-hoodie.jpg",
                3, 8, 5
            );
            
            Product product4 = Product.createProduct(
                "Gothic Cap",
                1500.0,
                "https://example.com/gothic-cap.jpg",
                20, 25, 15
            );
            
            productRepository.save(product1);
            productRepository.save(product2);
            productRepository.save(product3);
            productRepository.save(product4);
            
            log.info("Создано {} тестовых товаров", 4);
            
        } catch (Exception e) {
            log.error("Ошибка при создании тестовых товаров: {}", e.getMessage(), e);
        }
    }
    
    private void createSampleDigitalProducts() {
        try {
            // Проверяем, есть ли уже цифровые продукты в базе
            long digitalProductCount = digitalProductRepository.count();
            if (digitalProductCount > 0) {
                log.debug("Цифровые продукты уже существуют в базе данных ({}), пропускаем создание", digitalProductCount);
                return;
            }
            
            log.info("Создаем тестовые цифровые продукты...");
            
            // Создаем тестовые цифровые продукты
            DigitalProduct digitalProduct1 = DigitalProduct.builder()
                .name("Подписка Стандартная")
                .description("Месячная подписка с базовыми функциями")
                .price(599.0)
                .imageUrl("https://example.com/subscription-standard.jpg")
                .type("SUBSCRIPTION")
                .accessUrl("https://app.example.com/subscription/standard")
                .accessPeriodDays(30)
                .build();
            
            DigitalProduct digitalProduct2 = DigitalProduct.builder()
                .name("Подписка Премиум")
                .description("Месячная подписка с расширенными функциями")
                .price(999.0)
                .imageUrl("https://example.com/subscription-premium.jpg")
                .type("SUBSCRIPTION")
                .accessUrl("https://app.example.com/subscription/premium")
                .accessPeriodDays(30)
                .build();
            
            DigitalProduct digitalProduct3 = DigitalProduct.builder()
                .name("VIP Доступ")
                .description("Годовая подписка с VIP привилегиями")
                .price(5999.0)
                .imageUrl("https://example.com/subscription-vip.jpg")
                .type("SUBSCRIPTION")
                .accessUrl("https://app.example.com/subscription/vip")
                .accessPeriodDays(365)
                .build();
            
            digitalProductRepository.save(digitalProduct1);
            digitalProductRepository.save(digitalProduct2);
            digitalProductRepository.save(digitalProduct3);
            
            log.info("Создано {} тестовых цифровых продуктов", 3);
            
        } catch (Exception e) {
            log.error("Ошибка при создании тестовых цифровых продуктов: {}", e.getMessage(), e);
        }
    }
} 