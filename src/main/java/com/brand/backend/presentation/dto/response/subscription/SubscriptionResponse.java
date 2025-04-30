package com.brand.backend.presentation.dto.response.subscription;

import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private String activationCode;
    private SubscriptionLevel subscriptionLevel;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActive;
    private PurchasePlatform purchasePlatform;
} 