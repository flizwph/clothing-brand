package com.brand.backend.presentation.dto.request.subscription;

import com.brand.backend.domain.subscription.model.PurchasePlatform;
import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class CreateSubscriptionRequest {
    
    @NotNull(message = "ID пользователя не может быть пустым")
    private Long userId;
    
    @NotNull(message = "Уровень подписки не может быть пустым")
    private SubscriptionLevel level;
    
    @Positive(message = "Длительность подписки должна быть положительным числом")
    private int durationInDays;
    
    @NotNull(message = "Платформа покупки не может быть пустой")
    private PurchasePlatform platform;
} 