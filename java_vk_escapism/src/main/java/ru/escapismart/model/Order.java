package ru.escapismart.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = true)
    private Long userId;
    
    @Column(name = "order_text", length = 2000)
    private String orderText;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "amount")
    private Double amount;
    
    @Column(name = "discount_amount")
    private Double discountAmount;
    
    @Column(name = "final_amount")
    private Double finalAmount;
    
    @Column(name = "applied_promocode")
    private String appliedPromocode;
    
    @Column(name = "shipping_address", length = 1000)
    private String shippingAddress;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "customer_comment", length = 1000)
    private String customerComment;
    
    @Column(name = "admin_comment", length = 1000)
    private String adminComment;
    
    @Column(name = "product_ids")
    private String productIds;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "delivery_method")
    private String deliveryMethod;
    
    @Column(name = "tracking_number")
    private String trackingNumber;
    
    @Column(name = "shipping_cost")
    private Double shippingCost;
    
    @Column(name = "paid")
    private Boolean paid;
    
    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();
    
    public Order() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
        this.discountAmount = 0.0;
        this.shippingCost = 0.0;
        this.paid = false;
    }
    
    public Order(Long userId, String orderText) {
        this();
        this.userId = userId;
        this.orderText = orderText;
    }
    
    /**
     * Конструктор для создания заказа без userId
     * @param orderText Текст заказа
     */
    public Order(String orderText) {
        this();
        this.userId = 0L; // Значение по умолчанию
        this.orderText = orderText;
    }
    
    /**
     * Конструктор с тремя параметрами
     * @param userId ID пользователя
     * @param orderId ID заказа (записывается в orderText)
     * @param orderText Текст заказа
     */
    public Order(Long userId, String orderId, String orderText) {
        this();
        this.userId = userId;
        this.orderText = "Заказ #" + orderId + ": " + orderText;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getOrderText() {
        return orderText;
    }
    
    public void setOrderText(String orderText) {
        this.orderText = orderText;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
        calculateFinalAmount();
    }
    
    public Double getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
        calculateFinalAmount();
    }
    
    public Double getFinalAmount() {
        return finalAmount;
    }
    
    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }
    
    public String getAppliedPromocode() {
        return appliedPromocode;
    }
    
    public void setAppliedPromocode(String appliedPromocode) {
        this.appliedPromocode = appliedPromocode;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public String getCustomerComment() {
        return customerComment;
    }
    
    public void setCustomerComment(String customerComment) {
        this.customerComment = customerComment;
    }
    
    public String getAdminComment() {
        return adminComment;
    }
    
    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }
    
    public String getProductIds() {
        return productIds;
    }
    
    public void setProductIds(String productIds) {
        this.productIds = productIds;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public Double getShippingCost() {
        return shippingCost;
    }
    
    public void setShippingCost(Double shippingCost) {
        this.shippingCost = shippingCost;
        calculateFinalAmount();
    }
    
    public Boolean isPaid() {
        return paid;
    }
    
    public void setPaid(Boolean paid) {
        this.paid = paid;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }
    
    /**
     * Применить промокод к заказу
     * @param promocodeCode код промокода
     * @param discountAmount сумма скидки
     */
    public void applyPromocode(String promocodeCode, double discountAmount) {
        this.appliedPromocode = promocodeCode;
        this.discountAmount = discountAmount;
        calculateFinalAmount();
    }
    
    /**
     * Рассчитать итоговую сумму заказа с учетом скидки и доставки
     */
    private void calculateFinalAmount() {
        if (amount == null) {
            amount = 0.0;
        }
        
        if (discountAmount == null) {
            discountAmount = 0.0;
        }
        
        if (shippingCost == null) {
            shippingCost = 0.0;
        }
        
        // Итоговая сумма = стоимость товаров - скидка + стоимость доставки
        finalAmount = amount - discountAmount + shippingCost;
        
        // Итоговая сумма не может быть отрицательной
        if (finalAmount < 0) {
            finalAmount = 0.0;
        }
    }
    
    /**
     * Получить человекочитаемый статус заказа
     */
    public String getReadableStatus() {
        if (status == null) {
            return "Неизвестно";
        }
        
        switch (status) {
            case "PENDING":
                return "Ожидает подтверждения";
            case "CONFIRMED":
                return "Подтвержден";
            case "PROCESSING":
                return "В обработке";
            case "SHIPPED":
                return "Отправлен";
            case "DELIVERED":
                return "Доставлен";
            case "COMPLETED":
                return "Завершен";
            case "CANCELLED":
                return "Отменен";
            case "REFUNDED":
                return "Возвращен";
            default:
                return status;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
} 