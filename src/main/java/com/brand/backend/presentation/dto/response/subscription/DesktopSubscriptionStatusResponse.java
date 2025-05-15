package com.brand.backend.presentation.dto.response.subscription;

import com.brand.backend.domain.subscription.model.SubscriptionLevel;
import com.brand.backend.domain.subscription.model.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO для возврата статуса подписки desktop-приложения
 */
@Data
@Builder
public class DesktopSubscriptionStatusResponse {
    /**
     * Статус подписки
     */
    private SubscriptionStatus status;
    
    /**
     * Уровень подписки
     */
    private SubscriptionLevel level;
    
    /**
     * Дата активации подписки
     */
    private LocalDateTime activationDate;
    
    /**
     * Дата окончания срока действия подписки
     */
    private LocalDateTime expirationDate;
} 