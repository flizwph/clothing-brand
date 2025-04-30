package com.brand.backend.domain.subscription.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат активации подписки по коду
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionActivationResult {
    
    /**
     * Флаг успешной активации
     */
    private boolean success;
    
    /**
     * Сообщение об ошибке (если активация не удалась)
     */
    private String errorMessage;
    
    /**
     * Активированная подписка (если активация удалась)
     */
    private Subscription subscription;
    
    /**
     * Создает успешный результат активации
     * 
     * @param subscription активированная подписка
     * @return результат активации
     */
    public static SubscriptionActivationResult success(Subscription subscription) {
        return SubscriptionActivationResult.builder()
                .success(true)
                .subscription(subscription)
                .build();
    }
    
    /**
     * Создает результат с ошибкой активации
     * 
     * @param errorMessage сообщение об ошибке
     * @return результат активации
     */
    public static SubscriptionActivationResult error(String errorMessage) {
        return SubscriptionActivationResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
} 