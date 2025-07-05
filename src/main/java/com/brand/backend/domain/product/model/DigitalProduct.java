package com.brand.backend.domain.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность цифрового продукта
 */
@Entity
@Table(name = "digital_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "access_url")
    private String accessUrl;

    @Column(name = "access_period_days")
    private Integer accessPeriodDays;
    
    // Новые поля для маркета
    @Column(name = "category")
    private String category; // GAMES, ACCOUNTS, SOFTWARE, SUBSCRIPTIONS
    
    @Column(name = "platform")
    private String platform; // STEAM, EPIC, NETFLIX, SPOTIFY, etc.
    
    @Column(name = "region")
    private String region; // RU, EU, GLOBAL, etc.
    
    @Column(name = "delivery_method")
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod = DeliveryMethod.INSTANT;
    
    @Column(name = "auto_delivery")
    private Boolean autoDelivery = true;
    
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;
    
    @Column(name = "min_stock_alert")
    private Integer minStockAlert = 5;
    
    @Column(name = "supplier_id")
    private Long supplierId;
    
    @Column(name = "external_product_id")
    private String externalProductId; // ID в системе поставщика

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Проверяет, есть ли товар в наличии
     */
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
    
    /**
     * Проверяет, нужно ли пополнить запас
     */
    public boolean needsRestocking() {
        return stockQuantity != null && minStockAlert != null && 
               stockQuantity <= minStockAlert;
    }
    
    /**
     * Уменьшает количество на складе
     */
    public void decreaseStock(int quantity) {
        if (stockQuantity != null) {
            stockQuantity = Math.max(0, stockQuantity - quantity);
        }
    }
    
    /**
     * Увеличивает количество на складе
     */
    public void increaseStock(int quantity) {
        if (stockQuantity == null) {
            stockQuantity = quantity;
        } else {
            stockQuantity += quantity;
        }
    }
}

/**
 * Способ доставки цифрового товара
 */
enum DeliveryMethod {
    INSTANT,        // Мгновенная автоматическая доставка
    MANUAL,         // Ручная обработка администратором
    API_INTEGRATION // Через API поставщика
} 