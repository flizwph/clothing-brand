package com.brand.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Модель корзины покупок пользователя
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart {
    private String id;
    private String userId;
    
    @Builder.Default
    private List<ShoppingCartItem> items = new ArrayList<>();
    
    /**
     * Расчет общей стоимости корзины
     */
    public BigDecimal getTotal() {
        return items.stream()
                .map(ShoppingCartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Добавление товара в корзину
     */
    public void addItem(ShoppingCartItem item) {
        boolean itemExists = false;
        
        // Проверяем, есть ли уже этот товар в корзине
        for (ShoppingCartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                // Если товар уже есть, увеличиваем количество
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                itemExists = true;
                break;
            }
        }
        
        // Если товара еще нет в корзине, добавляем его
        if (!itemExists) {
            // Генерируем ID для нового элемента корзины, если его нет
            if (item.getId() == null) {
                item.setId(UUID.randomUUID().toString());
            }
            items.add(item);
        }
    }
    
    /**
     * Удаление товара из корзины по ID
     */
    public void removeItem(String itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
    }
    
    /**
     * Обновление количества товара в корзине
     */
    public void updateItemQuantity(String itemId, int quantity) {
        for (ShoppingCartItem item : items) {
            if (item.getId().equals(itemId)) {
                item.setQuantity(quantity);
                break;
            }
        }
    }
    
    /**
     * Очистка корзины
     */
    public void clear() {
        items.clear();
    }
    
    /**
     * Проверка, пуста ли корзина
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Получение количества товаров в корзине
     */
    public int getItemCount() {
        return items.stream()
                .mapToInt(ShoppingCartItem::getQuantity)
                .sum();
    }
} 