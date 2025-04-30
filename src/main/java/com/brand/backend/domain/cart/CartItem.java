package com.brand.backend.domain.cart;

import com.brand.backend.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String id;
    private Product product;
    private int quantity;
    
    public double getSubtotal() {
        return product.getPrice() * quantity;
    }
    
    public String getProductId() {
        return product != null ? product.getId() : null;
    }
} 