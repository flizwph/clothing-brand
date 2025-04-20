package com.brand.backend.infrastructure.integration.telegram.user.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель корзины покупок
 */
@Data
public class Cart {
    private final List<CartItem> items = new ArrayList<>();
    
    /**
     * Добавляет товар в корзину
     * 
     * @param item товар
     */
    public void addItem(CartItem item) {
        // Проверяем, есть ли уже такой товар с таким размером в корзине
        for (CartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId()) && 
                existingItem.getSize().equals(item.getSize())) {
                // Увеличиваем количество
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                return;
            }
        }
        
        // Если такого товара еще нет, добавляем новый
        items.add(item);
    }
    
    /**
     * Удаляет товар из корзины
     * 
     * @param productId ID товара
     * @param size размер товара
     * @return true, если товар был удален
     */
    public boolean removeItem(Long productId, String size) {
        return items.removeIf(item -> 
            item.getProductId().equals(productId) && item.getSize().equals(size)
        );
    }
    
    /**
     * Очищает корзину
     */
    public void clear() {
        items.clear();
    }
    
    /**
     * Проверяет, пуста ли корзина
     * 
     * @return true, если корзина пуста
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Возвращает общую стоимость товаров в корзине
     * 
     * @return общая стоимость
     */
    public double getTotalPrice() {
        return items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
    
    /**
     * Возвращает общее количество товаров в корзине
     * 
     * @return общее количество
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
} 