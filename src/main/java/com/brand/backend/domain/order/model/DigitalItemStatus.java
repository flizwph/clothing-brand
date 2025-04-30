package com.brand.backend.domain.order.model;

/**
 * Статусы элемента цифрового заказа
 */
public enum DigitalItemStatus {
    /**
     * Новый - создан, но еще не активирован
     */
    NEW,
    
    /**
     * Активирован пользователем
     */
    ACTIVATED,
    
    /**
     * Срок действия истек
     */
    EXPIRED,
    
    /**
     * Отменен
     */
    CANCELLED
} 