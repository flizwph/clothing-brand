package com.brand.backend.infrastructure.integration.telegram.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель представления товара в корзине пользователя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    
    /**
     * Идентификатор товара
     */
    private Long productId;
    
    /**
     * Название товара
     */
    private String productName;
    
    /**
     * Выбранный размер
     */
    private String size;
    
    /**
     * Количество товара
     */
    private int quantity;
    
    /**
     * Цена за единицу товара
     */
    private double price;
    
    /**
     * URL изображения товара
     */
    private String imageUrl;
    
    /**
     * Рассчитывает общую стоимость позиции (цена * количество)
     * 
     * @return общая стоимость
     */
    public double getTotalPrice() {
        return price * quantity;
    }
    
    @Override
    public String toString() {
        return String.format("%s (размер: %s) - %d шт. x %.2f руб. = %.2f руб.", 
                productName, size, quantity, price, getTotalPrice());
    }
} 