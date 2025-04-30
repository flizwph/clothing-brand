package com.brand.backend.application.order.service;

import com.brand.backend.domain.order.model.CartItem;
import java.util.List;

/**
 * Сервис для работы с корзиной пользователя
 */
public interface CartService {
    
    /**
     * Получает содержимое корзины пользователя
     *
     * @param userId ID пользователя
     * @return список товаров в корзине
     */
    List<CartItem> getCartItems(String userId);
    
    /**
     * Добавляет товар в корзину
     *
     * @param userId ID пользователя
     * @param productId ID товара
     * @param size размер товара
     * @param quantity количество
     */
    void addToCart(String userId, Long productId, String size, Integer quantity);
    
    /**
     * Удаляет товар из корзины
     *
     * @param userId ID пользователя
     * @param productId ID товара
     * @param size размер товара
     */
    void removeFromCart(String userId, Long productId, String size);
    
    /**
     * Очищает корзину пользователя
     *
     * @param userId ID пользователя
     */
    void clearCart(String userId);
} 