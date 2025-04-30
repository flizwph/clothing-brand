package com.brand.backend.application.product.service.impl;

import com.brand.backend.application.product.service.DigitalProductService;
import com.brand.backend.domain.product.model.DigitalProduct;
import com.brand.backend.domain.product.repository.DigitalProductRepository;
import com.brand.backend.domain.order.model.DigitalOrderItem;
import com.brand.backend.domain.order.repository.DigitalOrderItemRepository;
import com.brand.backend.domain.user.model.User;
import com.brand.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Реализация сервиса для работы с цифровыми продуктами
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalProductServiceImpl implements DigitalProductService {

    private final DigitalProductRepository digitalProductRepository;
    private final DigitalOrderItemRepository digitalOrderItemRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public List<DigitalProduct> getAllDigitalProducts() {
        log.debug("Получение всех цифровых продуктов");
        return digitalProductRepository.findAll();
    }

    @Override
    public Optional<DigitalProduct> getDigitalProductById(Long id) {
        log.debug("Получение цифрового продукта по ID: {}", id);
        return digitalProductRepository.findById(id);
    }

    @Override
    public List<DigitalProduct> getDigitalProductsByType(String type) {
        log.debug("Получение цифровых продуктов по типу: {}", type);
        return digitalProductRepository.findByType(type);
    }

    @Override
    @Transactional
    public DigitalProduct createDigitalProduct(String name, String description, double price, 
                                            String imageUrl, String type, String accessUrl, 
                                            Integer accessPeriodDays) {
        log.info("Создание нового цифрового продукта: {}", name);
        
        DigitalProduct product = DigitalProduct.builder()
                .name(name)
                .description(description)
                .price(price)
                .imageUrl(imageUrl)
                .type(type)
                .accessUrl(accessUrl)
                .accessPeriodDays(accessPeriodDays)
                .build();
        
        return digitalProductRepository.save(product);
    }

    @Override
    @Transactional
    public DigitalProduct updateDigitalProduct(Long id, String name, String description, double price, 
                                            String imageUrl, String type, String accessUrl, 
                                            Integer accessPeriodDays) {
        log.info("Обновление цифрового продукта с ID: {}", id);
        
        DigitalProduct existingProduct = digitalProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Цифровой продукт не найден с ID: " + id));
        
        existingProduct.setName(name);
        existingProduct.setDescription(description);
        existingProduct.setPrice(price);
        existingProduct.setImageUrl(imageUrl);
        existingProduct.setType(type);
        existingProduct.setAccessUrl(accessUrl);
        existingProduct.setAccessPeriodDays(accessPeriodDays);
        
        return digitalProductRepository.save(existingProduct);
    }

    @Override
    @Transactional
    public void deleteDigitalProduct(Long id) {
        log.info("Удаление цифрового продукта с ID: {}", id);
        digitalProductRepository.deleteById(id);
    }

    @Override
    public boolean userHasAccessToProduct(Long userId, Long productId) {
        log.debug("Проверка доступа пользователя {} к продукту {}", userId, productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + userId));
        
        return digitalOrderItemRepository.findByUserAndProductId(user, productId)
                .stream()
                .anyMatch(DigitalOrderItem::isActive);
    }

    @Override
    @Transactional
    public void grantAccessToProduct(Long userId, Long productId) {
        log.info("Предоставление доступа пользователю {} к продукту {}", userId, productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + userId));
        
        DigitalProduct product = digitalProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Цифровой продукт не найден с ID: " + productId));
        
        // Проверка, есть ли уже активный доступ
        boolean hasActiveAccess = digitalOrderItemRepository.findByUserAndProductId(user, productId)
                .stream()
                .anyMatch(item -> item.isActive());
        
        if (!hasActiveAccess) {
            // Создаем цифровой элемент заказа (без привязки к конкретному заказу для прямого доступа)
            DigitalOrderItem item = new DigitalOrderItem();
            item.setUser(user);
            item.setDigitalProduct(product);
            item.setActivationCode(generateActivationCode());
            item.setPrice(0.0); // Бесплатный доступ, так как предоставляется напрямую
            item.activate(); // Активируем сразу
            
            digitalOrderItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void revokeAccessToProduct(Long userId, Long productId) {
        log.info("Отзыв доступа у пользователя {} к продукту {}", userId, productId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден с ID: " + userId));
        
        List<DigitalOrderItem> activeItems = digitalOrderItemRepository.findByUserAndProductId(user, productId)
                .stream()
                .filter(item -> item.isActive())
                .toList();
        
        for (DigitalOrderItem item : activeItems) {
            // Устанавливаем дату истечения на текущий момент, чтобы доступ прекратился
            item.setExpirationDate(LocalDateTime.now());
            digitalOrderItemRepository.save(item);
        }
    }

    @Override
    public String generateActivationCode() {
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
} 