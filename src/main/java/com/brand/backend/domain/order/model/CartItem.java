package com.brand.backend.domain.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Модель элемента корзины
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    
    // ID элемента корзины
    private String id;
    
    // ID товара
    private Long productId;
    
    // Название товара
    private String productName;
    
    // Цена товара
    private BigDecimal price;
    
    // Размер товара (S, M, L, XL)
    private String size;
    
    // Количество
    private Integer quantity;
    
    // URL изображения товара
    private String imageUrl;
    
    /**
     * Рассчитывает подытог для элемента корзины
     * 
     * @return подытог (цена * количество)
     */
    public BigDecimal getSubtotal() {
        return price != null && quantity != null 
            ? price.multiply(BigDecimal.valueOf(quantity)) 
            : BigDecimal.ZERO;
    }
} 