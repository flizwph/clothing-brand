package com.brand.backend.infrastructure.integration.telegram.user.service;

import com.brand.backend.domain.product.model.Product;
import com.brand.backend.domain.product.repository.ProductRepository;
import com.brand.backend.infrastructure.integration.telegram.user.model.CartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для управления корзиной пользователя в Telegram боте.
 * Хранит временные данные о товарах в корзине для каждого пользователя.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    
    private final ProductRepository productRepository;
    
    // Карта для хранения корзин пользователей: ключ - chatId, значение - список товаров
    private final Map<Long, List<CartItem>> userCarts = new HashMap<>();
    
    /**
     * Добавляет товар в корзину пользователя по ID товара
     *
     * @param chatId идентификатор чата пользователя
     * @param productId идентификатор товара
     * @param size размер товара
     * @param quantity количество товара
     * @return true если товар успешно добавлен, false в противном случае
     */
    public boolean addProductToCart(Long chatId, Long productId, String size, int quantity) {
        try {
            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                log.error("Товар с ID {} не найден", productId);
                return false;
            }
            
            Product product = productOptional.get();
            
            if (quantity <= 0) {
                log.error("Некорректное количество товара: {}", quantity);
                return false;
            }
            
            // Проверяем наличие товара с нужным размером
            if (!product.getSizes().contains(size)) {
                log.error("Размер {} не доступен для товара {}", size, product.getName());
                return false;
            }
            
            CartItem item = CartItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .size(size)
                    .price(product.getPrice())
                    .quantity(quantity)
                    .imageUrl(product.getImageUrl())
                    .build();
            
            return addToCart(chatId, item);
        } catch (Exception e) {
            log.error("Ошибка при добавлении товара в корзину: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Добавляет товар в корзину пользователя.
     * Если товар с таким productId и размером уже существует, увеличивает его количество.
     *
     * @param chatId идентификатор чата пользователя
     * @param item товар для добавления в корзину
     * @return true если товар успешно добавлен, false в противном случае
     */
    public boolean addToCart(Long chatId, CartItem item) {
        try {
            List<CartItem> cart = userCarts.computeIfAbsent(chatId, k -> new ArrayList<>());
            
            // Проверяем, есть ли уже такой товар с таким размером в корзине
            boolean itemExists = false;
            for (CartItem existingItem : cart) {
                if (existingItem.getProductId().equals(item.getProductId()) && 
                    existingItem.getSize().equals(item.getSize())) {
                    // Увеличиваем количество существующего товара
                    existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                    itemExists = true;
                    break;
                }
            }
            
            // Если товара еще нет в корзине, добавляем его
            if (!itemExists) {
                cart.add(item);
            }
            
            log.info("Товар добавлен в корзину пользователя {}: {}", chatId, item);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при добавлении товара в корзину: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Получает содержимое корзины пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @return список товаров в корзине
     */
    public List<CartItem> getCartItems(Long chatId) {
        return userCarts.getOrDefault(chatId, new ArrayList<>());
    }
    
    /**
     * Удаляет товар из корзины пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @param index индекс товара в корзине
     * @return true если товар успешно удален, false в противном случае
     */
    public boolean removeFromCart(Long chatId, int index) {
        List<CartItem> cart = userCarts.getOrDefault(chatId, new ArrayList<>());
        if (index >= 0 && index < cart.size()) {
            CartItem removedItem = cart.remove(index);
            log.info("Товар удален из корзины пользователя {}: {}", chatId, removedItem);
            return true;
        }
        return false;
    }
    
    /**
     * Изменяет количество товара в корзине пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @param index индекс товара в корзине
     * @param quantity новое количество
     * @return true если количество успешно изменено, false в противном случае
     */
    public boolean updateQuantity(Long chatId, int index, int quantity) {
        if (quantity <= 0) {
            return removeFromCart(chatId, index);
        }
        
        List<CartItem> cart = userCarts.getOrDefault(chatId, new ArrayList<>());
        if (index >= 0 && index < cart.size()) {
            cart.get(index).setQuantity(quantity);
            log.info("Обновлено количество товара в корзине пользователя {}, индекс {}: {}", 
                    chatId, index, quantity);
            return true;
        }
        return false;
    }
    
    /**
     * Очищает корзину пользователя.
     *
     * @param chatId идентификатор чата пользователя
     */
    public void clearCart(Long chatId) {
        userCarts.remove(chatId);
        log.info("Корзина пользователя {} очищена", chatId);
    }
    
    /**
     * Проверяет, пуста ли корзина пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @return true если корзина пуста, false в противном случае
     */
    public boolean isCartEmpty(Long chatId) {
        List<CartItem> cart = userCarts.getOrDefault(chatId, new ArrayList<>());
        return cart.isEmpty();
    }
    
    /**
     * Рассчитывает общую стоимость товаров в корзине.
     *
     * @param chatId идентификатор чата пользователя
     * @return общая стоимость товаров
     */
    public double calculateTotal(Long chatId) {
        List<CartItem> cart = userCarts.getOrDefault(chatId, new ArrayList<>());
        return cart.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
} 