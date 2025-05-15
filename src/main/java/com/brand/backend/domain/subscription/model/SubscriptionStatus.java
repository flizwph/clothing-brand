package com.brand.backend.domain.subscription.model;

/**
 * Статус подписки для desktop-приложения
 */
public enum SubscriptionStatus {
    ACTIVE,     // Активная подписка
    EXPIRED,    // Истекшая подписка
    INACTIVE,   // Неактивная подписка
    PENDING     // Ожидающая активации
} 