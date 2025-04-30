package com.brand.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Модель элемента корзины покупок
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCartItem {
    private String id;
    private String productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private String imageUrl;
    private String size;
    
    /**
     * Расчет стоимости позиции (цена * количество)
     */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
} 