package com.brand.backend.domain.order.model;

import com.brand.backend.domain.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель цифрового заказа
 */
@Entity
@Table(name = "digital_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "is_paid")
    private boolean paid;
    
    @Column(name = "payment_id")
    private String paymentId;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DigitalOrderItem> items = new ArrayList<>();
    
    /**
     * Инициализация списка элементов если он null
     */
    private void ensureItemsInitialized() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
    }
    
    /**
     * Добавить позицию в заказ
     * 
     * @param item позиция для добавления
     */
    public void addItem(DigitalOrderItem item) {
        ensureItemsInitialized();
        items.add(item);
        item.setOrder(this);
    }
    
    /**
     * Удалить позицию из заказа
     * 
     * @param item позиция для удаления
     */
    public void removeItem(DigitalOrderItem item) {
        ensureItemsInitialized();
        items.remove(item);
        item.setOrder(null);
    }
    
    /**
     * Получить список элементов заказа (с защитой от null)
     */
    public List<DigitalOrderItem> getItems() {
        ensureItemsInitialized();
        return this.items;
    }
    
    /**
     * Вычислить общую стоимость заказа
     */
    public void calculateTotalPrice() {
        ensureItemsInitialized();
        this.totalPrice = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }
    
    /**
     * Проверить, содержит ли заказ указанный цифровой продукт
     * 
     * @param productId ID цифрового продукта
     * @return true, если заказ содержит продукт
     */
    public boolean containsProduct(Long productId) {
        ensureItemsInitialized();
        return items.stream()
                .anyMatch(item -> item.getDigitalProduct().getId().equals(productId));
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.orderNumber == null) {
            this.orderNumber = generateOrderNumber();
        }
        ensureItemsInitialized();
    }
    
    /**
     * Генерация номера заказа
     * 
     * @return сгенерированный номер заказа
     */
    private String generateOrderNumber() {
        return "DO-" + System.currentTimeMillis();
    }
} 