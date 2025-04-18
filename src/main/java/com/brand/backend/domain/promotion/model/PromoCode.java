package com.brand.backend.domain.promotion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "promo_codes")
public class PromoCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Проверяет, действителен ли промокод
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active 
                && usedCount < maxUses 
                && now.isAfter(startDate) 
                && (endDate == null || now.isBefore(endDate));
    }

    /**
     * Увеличивает счетчик использования промокода
     */
    public void incrementUsedCount() {
        this.usedCount++;
        if (this.usedCount >= this.maxUses) {
            this.active = false;
        }
    }
} 