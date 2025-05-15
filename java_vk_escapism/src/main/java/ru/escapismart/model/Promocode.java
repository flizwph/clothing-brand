package ru.escapismart.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Модель для хранения промокодов и скидок
 */
@Entity
@Table(name = "promocodes")
public class Promocode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", unique = true, nullable = false)
    private String code;
    
    @Column(name = "discount_percent")
    private Integer discountPercent;
    
    @Column(name = "discount_amount")
    private Double discountAmount;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_until")
    private LocalDateTime validUntil;
    
    @Column(name = "max_uses")
    private Integer maxUses;
    
    @Column(name = "current_uses")
    private Integer currentUses;
    
    @Column(name = "min_order_amount")
    private Double minOrderAmount;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public Promocode() {
        this.discountPercent = 0;
        this.discountAmount = 0.0;
        this.isActive = true;
        this.currentUses = 0;
        this.createdAt = LocalDateTime.now();
    }
    
    public Promocode(String code, Integer discountPercent) {
        this();
        this.code = code.toUpperCase();
        this.discountPercent = discountPercent;
    }
    
    public Promocode(String code, Double discountAmount) {
        this();
        this.code = code.toUpperCase();
        this.discountAmount = discountAmount;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code.toUpperCase();
    }
    
    public Integer getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }
    
    public Double getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public Boolean isActive() {
        return isActive;
    }
    
    public void setActive(Boolean active) {
        isActive = active;
    }
    
    public LocalDateTime getValidFrom() {
        return validFrom;
    }
    
    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }
    
    public LocalDateTime getValidUntil() {
        return validUntil;
    }
    
    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }
    
    public Integer getMaxUses() {
        return maxUses;
    }
    
    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }
    
    public Integer getCurrentUses() {
        return currentUses;
    }
    
    public void setCurrentUses(Integer currentUses) {
        this.currentUses = currentUses;
    }
    
    public Double getMinOrderAmount() {
        return minOrderAmount;
    }
    
    public void setMinOrderAmount(Double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Увеличить счетчик использований промокода
     */
    public void incrementUses() {
        if (this.currentUses == null) {
            this.currentUses = 0;
        }
        this.currentUses++;
    }
    
    /**
     * Проверка, можно ли использовать промокод
     * @return true если промокод действителен
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        
        // Проверка активности
        if (!isActive) {
            return false;
        }
        
        // Проверка периода действия
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }
        
        // Проверка количества использований
        if (maxUses != null && currentUses >= maxUses) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Рассчитать скидку для заказа
     * @param orderAmount Сумма заказа
     * @return Сумма скидки
     */
    public double calculateDiscount(double orderAmount) {
        // Проверка минимальной суммы заказа
        if (minOrderAmount != null && orderAmount < minOrderAmount) {
            return 0;
        }
        
        // Расчет скидки в процентах
        if (discountPercent != null && discountPercent > 0) {
            return orderAmount * discountPercent / 100.0;
        }
        
        // Фиксированная скидка
        if (discountAmount != null && discountAmount > 0) {
            return Math.min(discountAmount, orderAmount);
        }
        
        return 0;
    }
} 