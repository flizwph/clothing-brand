package com.brand.backend.domain.subscription.model;

import com.brand.backend.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Setter
@Getter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activation_code", nullable = false, unique = true)
    private String activationCode;

    @Column(name = "subscription_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionLevel subscriptionLevel;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "purchase_platform")
    @Enumerated(EnumType.STRING)
    private PurchasePlatform purchasePlatform;

    @Column(name = "last_check_date")
    private LocalDateTime lastCheckDate;
} 