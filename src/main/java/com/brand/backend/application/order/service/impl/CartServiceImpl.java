package com.brand.backend.application.order.service.impl;

import com.brand.backend.application.order.service.CartService;
import com.brand.backend.domain.order.model.CartItem;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация сервиса корзины
 * В реальном приложении этот сервис будет работать с базой данных
 */
@Service // Помечаем как сервис Spring
public class CartServiceImpl implements CartService {
    
    // Временное хранилище корзин пользователей (userId -> Map<productId_size, CartItem>)
    private final Map<String, Map<String, CartItem>> userCarts = new ConcurrentHashMap<>();
    
    @Override
    public List<CartItem> getCartItems(String userId) {
        Map<String, CartItem> cart = userCarts.getOrDefault(userId, new HashMap<>());
        return new ArrayList<>(cart.values());
    }
    
    @Override
    public void addToCart(String userId, Long productId, String size, Integer quantity) {
        Map<String, CartItem> cart = userCarts.computeIfAbsent(userId, k -> new HashMap<>());
        String key = productId + "_" + size;
        
        // Если товар уже есть в корзине, обновляем его количество
        CartItem existingItem = cart.get(key);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // Иначе добавляем новый товар
            cart.put(key, CartItem.builder()
                    .productId(productId)
                    .size(size)
                    .quantity(quantity)
                    .build());
        }
    }
    
    @Override
    public void removeFromCart(String userId, Long productId, String size) {
        Map<String, CartItem> cart = userCarts.get(userId);
        if (cart != null) {
            String key = productId + "_" + size;
            cart.remove(key);
            
            // Если корзина пуста, удаляем запись о корзине пользователя
            if (cart.isEmpty()) {
                userCarts.remove(userId);
            }
        }
    }
    
    @Override
    public void clearCart(String userId) {
        userCarts.remove(userId);
    }
} 